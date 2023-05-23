/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.core.backup.repository;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.lang.invoke.MethodHandles;

import com.google.common.base.Preconditions;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.NoLockFactory;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.DirectoryFactory;
import org.apache.solr.core.HdfsDirectoryFactory;
import org.apache.solr.store.hdfs.HdfsDirectory;
import org.apache.solr.store.hdfs.HdfsDirectory.HdfsIndexInput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HdfsBackupRepository implements BackupRepository {
  
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


  private static final String HDFS_UMASK_MODE_PARAM = "solr.hdfs.permissions.umask-mode";
  private static final String HDFS_COPY_BUFFER_SIZE_PARAM = "solr.hdfs.buffer.size";

  private HdfsDirectoryFactory factory;
  private Configuration hdfsConfig = null;
  private FileSystem fileSystem = null;
  private Path baseHdfsPath = null;
  private NamedList config = null;
  protected int copyBufferSize = HdfsDirectory.DEFAULT_BUFFER_SIZE;

  @SuppressWarnings("rawtypes")
  @Override
  public void init(NamedList args) {
    this.config = args;

    // Configure the size of the buffer used for copying index files to/from HDFS, if specified.
    if (args.get(HDFS_COPY_BUFFER_SIZE_PARAM) != null) {
      this.copyBufferSize = (Integer)args.get(HDFS_COPY_BUFFER_SIZE_PARAM);
      if (this.copyBufferSize <= 0) {
        throw new IllegalArgumentException("Value of " + HDFS_COPY_BUFFER_SIZE_PARAM + " must be > 0");
      }
    
    }

    String hdfsSolrHome = (String) Objects.requireNonNull(args.get(HdfsDirectoryFactory.HDFS_HOME),
        "Please specify " + HdfsDirectoryFactory.HDFS_HOME + " property.");
    Path path = new Path(hdfsSolrHome);
    while (path != null) { // Compute the path of root file-system (without requiring an additional system property).
      baseHdfsPath = path;
      path = path.getParent();
    }
    
    // We don't really need this factory instance. But we want to initialize it here to
    // make sure that all HDFS related initialization is at one place (and not duplicated here).
    factory = new HdfsDirectoryFactory();
    factory.init(args);
    this.hdfsConfig = factory.getConf(new Path(hdfsSolrHome));

    // Configure the umask mode if specified.
    if (args.get(HDFS_UMASK_MODE_PARAM) != null) {
      String umaskVal = (String)args.get(HDFS_UMASK_MODE_PARAM);
      this.hdfsConfig.set(FsPermission.UMASK_LABEL, umaskVal);
    }

    try {
      this.fileSystem = FileSystem.get(this.baseHdfsPath.toUri(), this.hdfsConfig);
    } catch (IOException e) {
      throw new SolrException(ErrorCode.SERVER_ERROR, e);
    }
    log.info("[MNP] called HdfsBackupRepository.init, HDFS_COPY_BUFFER_SIZE_PARAM:{}, hdfsSolrHome:{}, HDFS_UMASK_MODE_PARAM:{} ",HDFS_COPY_BUFFER_SIZE_PARAM,hdfsSolrHome,HDFS_UMASK_MODE_PARAM);
  }


  public void close() throws IOException {

    log.info("[MNP] called HdfsBackupRepository.close");
  
    if (this.fileSystem != null) {
      log.info("[MNP] called HdfsBackupRepository.close, fileSystem is not null");
      this.fileSystem.close();
    }
    if (this.factory != null) {
      log.info("[MNP] called HdfsBackupRepository.close, factory is not null");
      this.factory.close();
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getConfigProperty(String name) {
    return (T) this.config.get(name);
  }

  @Override
  public URI createURI(String location) {
    Objects.requireNonNull(location);

    URI result = null;
    try {
      result = new URI(location);
      if (!result.isAbsolute()) {
        result = resolve(this.baseHdfsPath.toUri(), location);
      }
    } catch (URISyntaxException ex) {
      result = resolve(this.baseHdfsPath.toUri(), location);
    }

    return result;
  }

  @Override
  public URI resolve(URI baseUri, String... pathComponents) {
    Preconditions.checkArgument(baseUri.isAbsolute());

    Path result = new Path(baseUri);
    for (String path : pathComponents) {
      result = new Path(result, path);
    }

    return result.toUri();
  }

  @Override
  public boolean exists(URI path) throws IOException {

    boolean output=this.fileSystem.exists(new Path(path));
    log.info("[MNP] called HdfsBackupRepository.exists, path:{}, result:{}",path.toURL(),output);
    return output;
  }

  @Override
  public PathType getPathType(URI path) throws IOException {
    log.info("[MNP] called HdfsBackupRepository.getPathType, path:{} ",path.toURL());
    return this.fileSystem.isDirectory(new Path(path)) ? PathType.DIRECTORY : PathType.FILE;
  }

  @Override
  public String[] listAll(URI path) throws IOException {
    FileStatus[] status = this.fileSystem.listStatus(new Path(path));
    String[] result = new String[status.length];
    
    String outLog="";
    for (int i = 0; i < status.length; i++) {
      result[i] = status[i].getPath().getName();
      outLog=outLog+" "+result[i];
    }
    log.info("[MNP] called HdfsBackupRepository.getPathType, path:{}, result:{} ",path.toURL(),outLog);
    return result;
  }

  @Override
  public IndexInput openInput(URI dirPath, String fileName, IOContext ctx) throws IOException {
    Path p = new Path(new Path(dirPath), fileName);
    log.info("[MNP] called HdfsBackupRepository.openInput, dirPath:{}, fileName:{}, ctx:{} ",dirPath.toURL(),fileName,"???");
    return new HdfsIndexInput(fileName, this.fileSystem, p, HdfsDirectory.DEFAULT_BUFFER_SIZE);
  }

  @Override
  public OutputStream createOutput(URI path) throws IOException {
    log.info("[MNP] called HdfsBackupRepository.createOutput, path:{}",path.toURL());
    return this.fileSystem.create(new Path(path));
  }

  @Override
  public void createDirectory(URI path) throws IOException {
    if (!this.fileSystem.mkdirs(new Path(path))) {
      log.info("[MNP] called HdfsBackupRepository.createDirectory, path:{}",path.toURL());
      throw new IOException("Unable to create a directory at following location " + path);
    }
  }

  @Override
  public void deleteDirectory(URI path) throws IOException {
    log.info("[MNP] called HdfsBackupRepository.deleteDirectory, path:{}",path.toURL());
    if (!this.fileSystem.delete(new Path(path), true)) {
      throw new IOException("Unable to delete a directory at following location " + path);
    }
  }

  @Override
  public void copyFileFrom(Directory sourceDir, String fileName, URI dest) throws IOException {
    log.info("[MNP] called HdfsBackupRepository.copyFileFrom, sourceDir:{}, fileName:{}, dest:{}",sourceDir, fileName,dest.toURL());
    try (HdfsDirectory dir = new HdfsDirectory(new Path(dest), NoLockFactory.INSTANCE,
        hdfsConfig, copyBufferSize)) {
      dir.copyFrom(sourceDir, fileName, fileName, DirectoryFactory.IOCONTEXT_NO_CACHE);
    }
  }

  @Override
  public void copyFileTo(URI sourceRepo, String fileName, Directory dest) throws IOException {
    log.info("[MNP] called HdfsBackupRepository.copyFileTo, sourceRepo:{}, fileName:{}, dest:{}",sourceRepo.toURL(),fileName,dest);
    try (HdfsDirectory dir = new HdfsDirectory(new Path(sourceRepo), NoLockFactory.INSTANCE,
        hdfsConfig, copyBufferSize)) {
      dest.copyFrom(dir, fileName, fileName, DirectoryFactory.IOCONTEXT_NO_CACHE);
    }
  }
}

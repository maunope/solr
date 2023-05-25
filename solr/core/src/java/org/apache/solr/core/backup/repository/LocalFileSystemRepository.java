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

import java.lang.invoke.MethodHandles;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.DirectoryFactory;

import com.google.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A concrete implementation of {@linkplain BackupRepository} interface supporting backup/restore of Solr indexes to a
 * local file-system. (Note - This can even be used for a shared file-system if it is exposed via a local file-system
 * interface e.g. NFS).
 */
public class LocalFileSystemRepository implements BackupRepository {
  private NamedList config = null;

  private static final org.slf4j.Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


  @Override
  public void init(NamedList args) {

    log.info("[MNP] called LocalFileSystemRepository.init, args:{}", args);
    this.config = args;
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
      log.info("[MNP] createURI location :{}", location);
      result = new URI(location);
      if (!result.isAbsolute()) {
        log.info("[MNP] createURI location is not absolute:{}", location);
        result = Paths.get(location).toUri();
        log.info("[MNP] createURI location new value is:{}", result.toString());
      }
    } catch (URISyntaxException ex) {
      result = Paths.get(location).toUri();
    }

    return result;
  }

  @Override
  public URI resolve(URI baseUri, String... pathComponents) {
    Preconditions.checkArgument(pathComponents.length > 0);

    Path result = Paths.get(baseUri);
    for (int i = 0; i < pathComponents.length; i++) {
      result = result.resolve(pathComponents[i]);
    }

    log.info("[MNP] called LocalFileSystemRepository.resolve, baseUri:{}, pathComponents:{}, result:{}", baseUri.toString(), pathComponents,result.toString() );
  
    return result.toUri();
  }

  @Override
  public void createDirectory(URI path) throws IOException {
    log.info("[MNP] called LocalFileSystemRepository.createDirectory, path:{}",path.toString());
    Files.createDirectory(Paths.get(path));
  }

  @Override
  public void deleteDirectory(URI path) throws IOException 
  {
  log.info("[MNP] called LocalFileSystemRepository.deleteDirectory, path:{}",path.toString());
    Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  @Override
  public boolean exists(URI path) throws IOException {
    boolean output=Files.exists(Paths.get(path));

    log.info("[MNP] called LocalFileSystemRepository.exists, path:{}",path.toString());
    return output;
  }

  @Override
  public IndexInput openInput(URI dirPath, String fileName, IOContext ctx) throws IOException {
    log.info("[MNP] called LocalFileSystemRepository.openInput, dirPath:{}, fileName:{}, IOContext:{}",dirPath.toString(),fileName,"???");
    try (FSDirectory dir = new SimpleFSDirectory(Paths.get(dirPath), NoLockFactory.INSTANCE)) {
      return dir.openInput(fileName, ctx);
    }
  }

  @Override
  public OutputStream createOutput(URI path) throws IOException {
    log.info("[MNP] called LocalFileSystemRepository.createOutput, path:{}",path.toString());
    return Files.newOutputStream(Paths.get(path));
  }

  @Override
  public String[] listAll(URI dirPath) throws IOException {
    try (FSDirectory dir = new SimpleFSDirectory(Paths.get(dirPath), NoLockFactory.INSTANCE)) {
      String[] output = dir.listAll();
      log.info("[MNP] called LocalFileSystemRepository.listAll, dirPath:{}, output:{}",dirPath.toString(),output.toString() );
      return output;
    }
  }

  @Override
  public PathType getPathType(URI path) throws IOException {
    log.info("[MNP] called LocalFileSystemRepository.getPathType, path:{}",path.toString());
    return Files.isDirectory(Paths.get(path)) ? PathType.DIRECTORY : PathType.FILE;
  }

  @Override
  public void copyFileFrom(Directory sourceDir, String fileName, URI dest) throws IOException {
    log.info("[MNP] called LocalFileSystemRepository.copyFileFrom, sourceDir:{}, fileName:{}, dest:{}",sourceDir,fileName ,dest.toString());
    try (FSDirectory dir = new SimpleFSDirectory(Paths.get(dest), NoLockFactory.INSTANCE)) {
      dir.copyFrom(sourceDir, fileName, fileName, DirectoryFactory.IOCONTEXT_NO_CACHE);
    }
  }

  @Override
  public void copyFileTo(URI sourceDir, String fileName, Directory dest) throws IOException {
    log.info("[MNP] called LocalFileSystemRepository.copyFileTo, sourceDir:{}, fileName:{}, dest:{} ",sourceDir.toString(), fileName, dest);
    try (FSDirectory dir = new SimpleFSDirectory(Paths.get(sourceDir), NoLockFactory.INSTANCE)) {
      dest.copyFrom(dir, fileName, fileName, DirectoryFactory.IOCONTEXT_NO_CACHE);
    }
  }

  @Override
  public void close() throws IOException {}
}

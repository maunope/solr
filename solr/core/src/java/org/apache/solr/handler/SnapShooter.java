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
package org.apache.solr.handler;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.store.Directory;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.DirectoryFactory.DirContext;
import org.apache.solr.core.IndexDeletionPolicyWrapper;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.backup.repository.BackupRepository;
import org.apache.solr.core.backup.repository.BackupRepository.PathType;
import org.apache.solr.core.backup.repository.LocalFileSystemRepository;
import org.apache.solr.core.snapshots.SolrSnapshotMetaDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p> Provides functionality equivalent to the snapshooter script </p>
 * This is no longer used in standard replication.
 *
 *
 * @since solr 1.4
 */
public class SnapShooter {
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private SolrCore solrCore;
  private String snapshotName = null;
  private String directoryName = null;
  private URI baseSnapDirPath = null;
  private URI snapshotDirPath = null;
  private BackupRepository backupRepo = null;
  private String commitName; // can be null

  @Deprecated
  public SnapShooter(SolrCore core, String location, String snapshotName) {
    String snapDirStr = null;
    // Note - This logic is only applicable to the usecase where a shared file-system is exposed via
    // local file-system interface (primarily for backwards compatibility). For other use-cases, users
    // will be required to specify "location" where the backup should be stored.
   log.info("[MNP] you should not be here");
    if (location == null) {
      snapDirStr = core.getDataDir();
    } else {
      snapDirStr = core.getCoreDescriptor().getInstanceDir().resolve(location).normalize().toString();
    }
    initialize(new LocalFileSystemRepository(), core, Paths.get(snapDirStr).toUri(), snapshotName, null);
  }

  public SnapShooter(BackupRepository backupRepo, SolrCore core, URI location, String snapshotName, String commitName) {
    log.info("[MNP] SnapShooter constructor with 5 params backupRepo:{}, core:{}, location:{}, snapshotName:{}, commitName:{}",backupRepo.toString(), core.toString(), location, snapshotName, commitName );
    initialize(backupRepo, core, location, snapshotName, commitName);
  }

  private void initialize(BackupRepository backupRepo, SolrCore core, URI location, String snapshotName, String commitName) {
    log.info("MNP XXXXX");
    log.info("MNP XXXXX core:{}, backupRepo:{}",core.toString(), backupRepo.toString());
    this.solrCore = Objects.requireNonNull(core);
    this.backupRepo = Objects.requireNonNull(backupRepo);
    this.baseSnapDirPath = location;
    this.snapshotName = snapshotName;
    log.info("MNP XXXXX");
    if (snapshotName != null) {
      log.info("MNP XXXXX2");
      directoryName = "snapshot." + snapshotName;
    } else {
      log.info("MNP XXXXX3");
      SimpleDateFormat fmt = new SimpleDateFormat(DATE_FMT, Locale.ROOT);
      log.info("MNP XXXXX4");
      directoryName = "snapshot." + fmt.format(new Date());
    }
    log.info("MNP XXXXX5");
    this.snapshotDirPath = backupRepo.resolve(location, directoryName);
    log.info("MNP XXXXX6");
    this.commitName = commitName;
  }

  public BackupRepository getBackupRepository() {
    return backupRepo;
  }

  /**
   * Gets the parent directory of the snapshots. This is the {@code location}
   * given in the constructor.
   */
  public URI getLocation() {
    return this.baseSnapDirPath;
  }

  public void validateDeleteSnapshot() {
    Objects.requireNonNull(this.snapshotName);

    boolean dirFound = false;
    String[] paths;
    try {
      paths = backupRepo.listAll(baseSnapDirPath);
      for (String path : paths) {
        if (path.equals(this.directoryName)
            && backupRepo.getPathType(baseSnapDirPath.resolve(path)) == PathType.DIRECTORY) {
          dirFound = true;
          break;
        }
      }
      if(dirFound == false) {
        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Snapshot " + snapshotName + " cannot be found in directory: " + baseSnapDirPath);
      }
    } catch (IOException e) {
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Unable to find snapshot " + snapshotName + " in directory: " + baseSnapDirPath, e);
    }
  }

  protected void deleteSnapAsync(final ReplicationHandler replicationHandler) {
    new Thread(() -> deleteNamedSnapshot(replicationHandler)).start();
  }

  public void validateCreateSnapshot() throws IOException {
    // Note - Removed the current behavior of creating the directory hierarchy.
    // Do we really need to provide this support?
    if (!backupRepo.exists(baseSnapDirPath)) {
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
          " Directory does not exist: " + snapshotDirPath);
    }

    if (backupRepo.exists(snapshotDirPath)) {
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
          "Snapshot directory already exists: " + snapshotDirPath);
    }
  }

  public NamedList createSnapshot() throws Exception {
    
    //log.info("[MNP] SnapShooter.createSnapshot presleep");
    IndexCommit indexCommit;
    //Thread.sleep(120000);
    //log.info("[MNP] SnapShooter.createSnapshot postsleep");
    if (commitName != null) {
      indexCommit = getIndexCommitFromName();
      log.info("[MNP] SnapShooter.createSnapshot a1");
      return createSnapshot(indexCommit);
    } else {
      log.info("[MNP] SnapShooter.createSnapshot b1");
      indexCommit = getIndexCommit();
      log.info("[MNP] SnapShooter.createSnapshot b2");
      IndexDeletionPolicyWrapper deletionPolicy = solrCore.getDeletionPolicy();
      log.info("[MNP] SnapShooter.createSnapshot b3");
      deletionPolicy.saveCommitPoint(indexCommit.getGeneration());
      log.info("[MNP] SnapShooter.createSnapshot b4");
      try {
        return createSnapshot(indexCommit);
      } finally {
        deletionPolicy.releaseCommitPoint(indexCommit.getGeneration());
        log.info("[MNP] SnapShooter.createSnapshot b6");
      }
    }
  }

  private IndexCommit getIndexCommit() throws IOException {
    IndexDeletionPolicyWrapper delPolicy = solrCore.getDeletionPolicy();
    IndexCommit indexCommit = delPolicy.getLatestCommit();
    if (indexCommit != null) {
      return indexCommit;
    }
    return solrCore.withSearcher(searcher -> searcher.getIndexReader().getIndexCommit());
  }

  private IndexCommit getIndexCommitFromName() throws IOException {
    assert commitName !=null;
    IndexCommit indexCommit;
    SolrSnapshotMetaDataManager snapshotMgr = solrCore.getSnapshotMetaDataManager();
    Optional<IndexCommit> commit = snapshotMgr.getIndexCommitByName(commitName);
    if (commit.isPresent()) {
      indexCommit = commit.get();
    } else {
      throw new SolrException(ErrorCode.BAD_REQUEST, "Unable to find an index commit with name " + commitName +
          " for core " + solrCore.getName());
    }
    return indexCommit;
  }

  public void createSnapAsync(final int numberToKeep, Consumer<NamedList> result) throws IOException {
    IndexCommit indexCommit;
    if (commitName != null) {
      indexCommit = getIndexCommitFromName();
    } else {
      indexCommit = getIndexCommit();
    }
    createSnapAsync(indexCommit, numberToKeep, result);
  }

  private void createSnapAsync(final IndexCommit indexCommit, final int numberToKeep, Consumer<NamedList> result) {
    //TODO should use Solr's ExecutorUtil
    new Thread(() -> {
      try {
        log.info("[MNP] SnapShooter.createSnapAsync 1");
        result.accept(createSnapshot(indexCommit));
        log.info("[MNP] SnapShooter.createSnapAsync 2");
      } catch (Exception e) {
        log.error("Exception while creating snapshot", e);
        NamedList snapShootDetails = new NamedList<>();
        snapShootDetails.add("exception", e.getMessage());
        result.accept(snapShootDetails);
      } finally {
        solrCore.getDeletionPolicy().releaseCommitPoint(indexCommit.getGeneration());
      }
      if (snapshotName == null) {
        try {
          deleteOldBackups(numberToKeep);
        } catch (IOException e) {
          log.warn("Unable to delete old snapshots ", e);
        }
      }
    }).start();

  }

  // note: remember to reserve the indexCommit first so it won't get deleted concurrently
  protected NamedList createSnapshot(final IndexCommit indexCommit) throws Exception {
    assert indexCommit != null;
    log.info("Creating backup snapshot " + (snapshotName == null ? "<not named>" : snapshotName) + " at " + baseSnapDirPath);
    boolean success = false;
    try {
      log.info("[MNP] SnapShooter.createSnapshot 1");
      NamedList<Object> details = new NamedList<>();
      details.add("startTime", new Date().toString());//bad; should be Instant.now().toString()
      log.info("[MNP] SnapShooter.createSnapshot 2");
      Collection<String> files = indexCommit.getFileNames();

      log.info("[MNP] SnapShooter.createSnapshot locking folder {} with lock type:{}",solrCore.getIndexDir(),solrCore.getSolrConfig().indexConfig.lockType);
      Directory dir = solrCore.getDirectoryFactory().get(solrCore.getIndexDir(), DirContext.DEFAULT, solrCore.getSolrConfig().indexConfig.lockType);
      log.info("[MNP] SnapShooter.createSnapshot 3");
      try {
        for(String fileName : files) {
          log.info("[MNP] SnapShooter.createSnapshot 4");
          log.info("[MNP] SnapShooter.createSnapshot 4 dir:{}, filename:{}, snapshotDirPath:{}",dir,fileName, snapshotDirPath);
          backupRepo.copyFileFrom(dir, fileName, snapshotDirPath);
        }
      } finally {
        log.info("[MNP] SnapShooter.createSnapshot  release folder lock on folder {}",solrCore.getIndexDir());
        solrCore.getDirectoryFactory().release(dir);
      }

      details.add("fileCount", files.size());
      details.add("status", "success");
      details.add("snapshotCompletedAt", new Date().toString());//bad; should be Instant.now().toString()
      details.add("snapshotName", snapshotName);
      log.info("[MNP] SnapShooter.createSnapshot 5");
      log.info("[MNP] SnapShooter.createSnapshot 5 details: {}", details);
      log.info("Done creating backup snapshot: " + (snapshotName == null ? "<not named>" : snapshotName) +
          " at " + baseSnapDirPath);
      success = true;
      return details;
    } finally {
      if (!success) {
        try {
          backupRepo.deleteDirectory(snapshotDirPath);
        } catch (Exception excDuringDelete) {
          log.warn("Failed to delete "+snapshotDirPath+" after snapshot creation failed due to: "+excDuringDelete);
        }
      }
    }
  }

  private void deleteOldBackups(int numberToKeep) throws IOException {
    String[] paths = backupRepo.listAll(baseSnapDirPath);
    List<OldBackupDirectory> dirs = new ArrayList<>();
    for (String f : paths) {
      if (backupRepo.getPathType(baseSnapDirPath.resolve(f)) == PathType.DIRECTORY) {
        OldBackupDirectory obd = new OldBackupDirectory(baseSnapDirPath, f);
        if (obd.getTimestamp().isPresent()) {
          dirs.add(obd);
        }
      }
    }
    if (numberToKeep > dirs.size() -1) {
      return;
    }
    Collections.sort(dirs);
    int i=1;
    for (OldBackupDirectory dir : dirs) {
      if (i++ > numberToKeep) {
        backupRepo.deleteDirectory(dir.getPath());
      }
    }
  }

  protected void deleteNamedSnapshot(ReplicationHandler replicationHandler) {
    log.info("Deleting snapshot: " + snapshotName);

    NamedList<Object> details = new NamedList<>();

    try {
      URI path = baseSnapDirPath.resolve("snapshot." + snapshotName);
      backupRepo.deleteDirectory(path);

      details.add("status", "success");
      details.add("snapshotDeletedAt", new Date().toString());

    } catch (IOException e) {
      details.add("status", "Unable to delete snapshot: " + snapshotName);
      log.warn("Unable to delete snapshot: " + snapshotName, e);
    }

    replicationHandler.snapShootDetails = details;
  }

  public static final String DATE_FMT = "yyyyMMddHHmmssSSS";

}

#!/bin/bash
# Copyright 2023 Google. This software is provided as-is, without warranty or representation for any use or purpose. 
# Your use of it is subject to your agreement with Google.

set -euxo pipefail

readonly DATAPROC_DIR="/usr/local/share/google/dataproc"

source "${DATAPROC_DIR}/bdutil/bdutil_logging.sh"
source "${DATAPROC_DIR}/bdutil/bdutil_retry.sh"
source "${DATAPROC_DIR}/bdutil/bdutil_metadata.sh"
source "${DATAPROC_DIR}/bdutil/bdutil_properties.sh"
source "${DATAPROC_DIR}/bdutil/bdutil_versions.sh"

readonly SOLR_HOME_DIR="/usr/lib/solr/server/solr"

BACKUP_SPEC="  <backup>\n\
    <repository name=\"local\" class=\"org.apache.solr.core.backup.repository.LocalFileSystemRepository\" default=\"false\">\n\
    <\/repository>\n\
  <\/backup>"
MATCH='<\/shardHandlerFactory>'
FILE="$SOLR_HOME_DIR/solr.xml"

function setup_solrxml() {
    sudo sed -i "s/$MATCH/$MATCH\n\n$BACKUP_SPEC/" $FILE

    sudo systemctl restart solr
}

setup_solrxml
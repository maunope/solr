#!/bin/bash
# Copyright 2023 Google. This software is provided as-is, without warranty or representation for any use or purpose. 
# Your use of it is subject to your agreement with Google.

set -euxo pipefail

readonly NFS_SERVER_HOSTNAME="$(/usr/share/google/get_metadata_value attributes/nfs_server_hostname)"
readonly NFS_SERVER_DIR="$(/usr/share/google/get_metadata_value attributes/nfs_server_dir)"
readonly LOCAL_DIR="/tmp/localsolarbackupmount"

function mount_nfs() {
    sudo mkdir $LOCAL_DIR
    sudo mount -t nfs ${NFS_SERVER_HOSTNAME}:${NFS_SERVER_DIR} $LOCAL_DIR
}

mount_nfs
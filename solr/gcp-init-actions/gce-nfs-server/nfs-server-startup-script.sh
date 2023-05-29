#! /bin/bash
sudo apt-get update
sudo apt install -y nfs-kernel-server
sudo mkdir /mnt/solrbackupsshare
sudo mkfs -t ext4 /dev/sdb
sudo mount /dev/sdb /mnt/solrbackupsshare
sudo chmod 777 /mnt/solrbackupsshare
echo "/mnt/solrbackupsshare *(rw,sync,no_subtree_check)" | sudo tee -a /etc/exports
sudo exportfs -a
sudo systemctl restart nfs-kernel-server
sudo systemctl restart nfs-server
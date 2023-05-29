export CLUSTER_NAME=solr-test
export N_MASTERS=3
export N_WORKERS=2

max_m=$(($N_MASTERS - 1))


gcloud compute ssh ${CLUSTER_NAME}-m-0 --zone europe-west3-b -- << EOF
sudo apt-get update
sudo apt install nfs-kernel-server
sudo mkdir /mnt/solrbackupsshare
sudo chmod 777 /mnt/solrbackupsshare
sudo sed -i 's/\/mnt\/solrbackupsshare 10.0.0.0\/8(rw,sync,no_subtree_check)//' /etc/exports
echo "/mnt/solrbackupsshare 10.0.0.0/8(rw,sync,no_subtree_check)" |sudo tee -a /etc/exports
sudo exportfs -a
sudo systemctl restart nfs-kernel-server
sudo systemctl restart nfs-server
EOF

for i in `seq 0 $max_m`
do
    echo "${CLUSTER_NAME}-m-${i}"
    gcloud compute ssh ${CLUSTER_NAME}-m-${i} --zone europe-west3-b -- << EOF
rm -r our-solr-8.1.1
mkdir our-solr-8.1.1
gsutil -m cp -r gs://chb-isp-hadoop-playground-jobs-src/solr_8.1.1/* our-solr-8.1.1

sudo rm /usr/lib/solr/lib/*
sudo rm -r /usr/lib/solr/lib/solrj-lib
sudo cp -r our-solr-8.1.1/* /usr/lib/solr/lib/

sudo rm /usr/lib/solr/server/solr-webapp/webapp/WEB-INF/lib/solr-core-8.1.1.jar
sudo rm /usr/lib/solr/server/solr-webapp/webapp/WEB-INF/lib/solr-solrj-8.1.1.jar
sudo cp our-solr-8.1.1/solr-core-8.1.1.jar /usr/lib/solr/server/solr-webapp/webapp/WEB-INF/lib/
sudo cp our-solr-8.1.1/solr-solrj-8.1.1.jar /usr/lib/solr/server/solr-webapp/webapp/WEB-INF/lib/

sudo cp our-solr-8.1.1/solr.xml /usr/lib/solr/server/solr/solr.xml

stat /usr/lib/solr/lib
stat /usr/lib/solr/lib/solrj-lib
stat /usr/lib/solr/server/solr-webapp/webapp/WEB-INF/lib/solr-core-8.1.1.jar
stat /usr/lib/solr/server/solr-webapp/webapp/WEB-INF/lib/solr-solrj-8.1.1.jar

sudo mkdir /tmp/localsolarbackupmount
sudo mount -t nfs ${CLUSTER_NAME}-m-0:/mnt/solrbackupsshare /tmp/localsolarbackupmount
sudo chmod 777 /tmp/localsolarbackupmount

sudo systemctl restart solr
EOF
done


max_w=$((N_WORKERS - 1))
for i in `seq 0 $max_w`
do
    echo "${CLUSTER_NAME}-w-${i}"
    gcloud compute ssh ${CLUSTER_NAME}-w-${i} --zone europe-west3-b -- << EOF
rm -r our-solr-8.1.1
mkdir our-solr-8.1.1
gsutil -m cp -r gs://chb-isp-hadoop-playground-jobs-src/solr_8.1.1/* our-solr-8.1.1

sudo rm /usr/lib/solr/lib/*
sudo rm -r /usr/lib/solr/lib/solrj-lib
sudo cp -r our-solr-8.1.1/* /usr/lib/solr/lib/

sudo rm /usr/lib/solr/server/solr-webapp/webapp/WEB-INF/lib/solr-core-8.1.1.jar
sudo rm /usr/lib/solr/server/solr-webapp/webapp/WEB-INF/lib/solr-solrj-8.1.1.jar
sudo cp our-solr-8.1.1/solr-core-8.1.1.jar /usr/lib/solr/server/solr-webapp/webapp/WEB-INF/lib/
sudo cp our-solr-8.1.1/solr-solrj-8.1.1.jar /usr/lib/solr/server/solr-webapp/webapp/WEB-INF/lib/

sudo cp our-solr-8.1.1/solr.xml /usr/lib/solr/server/solr/solr.xml

stat /usr/lib/solr/lib
stat /usr/lib/solr/lib/solrj-lib
stat /usr/lib/solr/server/solr-webapp/webapp/WEB-INF/lib/solr-core-8.1.1.jar
stat /usr/lib/solr/server/solr-webapp/webapp/WEB-INF/lib/solr-solrj-8.1.1.jar

sudo mkdir /tmp/localsolarbackupmount
sudo mount -t nfs ${CLUSTER_NAME}-m-0:/mnt/solrbackupsshare /tmp/localsolarbackupmount
sudo chmod 777 /tmp/localsolarbackupmount

sudo systemctl restart solr
EOF
done

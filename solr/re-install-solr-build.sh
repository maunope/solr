export CLUSTER_NAME=solr-test
export N_MASTERS=3
export N_WORKERS=2

max_m=$(($N_MASTERS - 1))


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
sudo rm /usr/lib/solr/lib/solr.xml

sudo rm /usr/lib/solr/server/solr-webapp/webapp/WEB-INF/lib/solr-core-8.1.1.jar
sudo rm /usr/lib/solr/server/solr-webapp/webapp/WEB-INF/lib/solr-solrj-8.1.1.jar
sudo cp our-solr-8.1.1/solr-core-8.1.1.jar /usr/lib/solr/server/solr-webapp/webapp/WEB-INF/lib/
sudo cp our-solr-8.1.1/solr-solrj-8.1.1.jar /usr/lib/solr/server/solr-webapp/webapp/WEB-INF/lib/

sudo cp our-solr-8.1.1/solr.xml /usr/lib/solr/server/solr/solr.xml 

stat /usr/lib/solr/lib
stat /usr/lib/solr/lib/solrj-lib
stat /usr/lib/solr/server/solr-webapp/webapp/WEB-INF/lib/solr-core-8.1.1.jar
stat /usr/lib/solr/server/solr-webapp/webapp/WEB-INF/lib/solr-solrj-8.1.1.jar

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

stat /usr/lib/solr/lib
stat /usr/lib/solr/lib/solrj-lib
stat /usr/lib/solr/server/solr-webapp/webapp/WEB-INF/lib/solr-core-8.1.1.jar
stat /usr/lib/solr/server/solr-webapp/webapp/WEB-INF/lib/solr-solrj-8.1.1.jar

sudo systemctl restart solr
EOF
done

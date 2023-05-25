cp solr.xml ./dist/solr.xml
gsutil -m  cp -r ./dist/* gs://chb-isp-hadoop-playground-jobs-src/solr_8.1.1
sh re-install-solr-build.sh


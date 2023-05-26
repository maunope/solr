export BCK_SHARE=/tmp/localsolarbackupmount/
export BCK_NAME=$(date +%s)




# Backup the collection
curl -H "Content-Type: application/json" \
    -X POST \
    --url "http://localhost:8983/solr/admin/collections?action=BACKUP&name=books_b_${BCK_NAME}&collection=books_0&repository=local&location=${BCK_SHARE}/"


# Check backup size on HDFS
hdfs dfs -du -s -h $BCK_HDFS

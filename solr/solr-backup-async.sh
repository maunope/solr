export BCK_HDFS=/backups
export BCK_NAME=$(date +%s)


# Backup the collection
curl -H "Content-Type: application/json" \
    -X POST \
    --url "http://localhost:8983/solr/admin/collections?action=BACKUP&name=books_b_${BCK_NAME}&collection=books_0&repository=hdfs&location=${BCK_HDFS}/&async=${BCK_NAME}"


# Poll the backup status
while true
do
    res=$(curl -s -H "Content-Type: application/json" \
        -X POST \
        --url "http://localhost:8983/solr/admin/collections?action=REQUESTSTATUS&requestid=${BCK_NAME}")
    
    status=$(echo $res | jq -r .status.state)
    echo $status

    if [ "$status" = "completed" ]; then
        echo $res
        break
    fi
    
    sleep 15
done


# Check backup size on HDFS
hdfs dfs -du -s -h $BCK_HDFS

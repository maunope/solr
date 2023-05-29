# Setup
export PROJECT_ID= #<PROJECT-ID>
export REGION=europe-west3
export ZONE=europe-west3-a
export VM_NAME=nfs-server
export SUBNET= #<SUBNET>
export DATA_DISK_SIZE=5000
export STARTUP_SCRIPT_GCS_URL= #gs://<GCS-URL>

gcloud config set project $PROJECT_ID
gcloud config set compute/region $REGION
gcloud config set compute/zone $ZONE


# Create a cluster with Solr
gcloud compute instances create $VM_NAME \
    --machine-type=e2-medium \
    --network-interface=subnet=${SUBNET} \
    --create-disk=auto-delete=yes,boot=yes,image=projects/debian-cloud/global/images/debian-11-bullseye-v20230509,mode=rw,size=10,type=projects/${PROJECT_ID}/zones/${ZONE}/diskTypes/pd-balanced \
    --create-disk=device-name=disk-1,mode=rw,name=disk-1,size=${DATA_DISK_SIZE},type=projects/${PROJECT_ID}/zones/${ZONE}/diskTypes/pd-standard \
    --metadata=startup-script-url=${STARTUP_SCRIPT_GCS_URL}

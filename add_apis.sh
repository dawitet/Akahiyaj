
#!/bin/bash

API_KEY="fc54c1bc-3a66-444c-b38b-8e8156284cd2"
PROJECT="akahidegn-79376"

APIS=(
"firebasehosting.googleapis.com"
"firebaserules.googleapis.com"
"sqladmin.googleapis.com"
"cloudconfig.googleapis.com"
"datastore.googleapis.com"
"fcmregistrations.googleapis.com"
"firebase.googleapis.com"
"firebaseappcheck.googleapis.com"
"firebaseappdistribution.googleapis.com"
"firebaseapphosting.googleapis.com"
"firebaseapptesters.googleapis.com"
"firebasedataconnect.googleapis.com"
"firebasedynamiclinks.googleapis.com"
"firebaseinappmessaging.googleapis.com"
"firebaseinstallations.googleapis.com"
"firebaseml.googleapis.com"
"firebaseremoteconfig.googleapis.com"
"firebaseremoteconfigrealtime.googleapis.com"
"firebasestorage.googleapis.com"
"firebasevertexai.googleapis.com"
"firestore.googleapis.com"
"identitytoolkit.googleapis.com"
"logging.googleapis.com"
"mlkit.googleapis.com"
"play.googleapis.com"
"securetoken.googleapis.com"
)

for API in "${APIS[@]}"
do
  echo "Adding $API..."
  gcloud services api-keys update $API_KEY --project=$PROJECT --api-target=service=$API
done

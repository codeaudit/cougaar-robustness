#!/bin/bash

zip=$1
cp -R operator/ staging/
cd staging
chmod -R 775 operator/
zip -r ../${zip} .
cd ..



#!/bin/sh

curl --get -d 'ASSETNAME=TestAgent&Submit=Create' 'http://localhost:8801/$TestAgent/PublishServlet/myForm'

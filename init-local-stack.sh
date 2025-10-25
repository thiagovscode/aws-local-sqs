#!/bin/sh

echo "Init localStack"

awslocal sqs create-queue --queue-name pizza-order

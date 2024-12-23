#!/bin/bash

source .env

read QUERY

java -jar Server.jar $QUERY $psql_pass

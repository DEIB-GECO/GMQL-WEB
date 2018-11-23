#!/usr/bin/env bash

gmql_web_zip_path=`ls ./target/universal/*.zip`
gmql_web_root_zip_path="./gmql_web.zip"

mv ${gmql_web_zip_path} ${gmql_web_root_zip_path}
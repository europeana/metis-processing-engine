#!/bin/bash
set -e
if [ -z "${S3_ACCESS_KEY}" ] || [ -z "${S3_SECRET_KEY}" ] || [ -z "${S3_BUCKET}" ] || [ -z "${S3_ENDPOINT}" ] || [ -z "${S3_REGION}" ]; then
  echo "Some S3 variables not found in environment"
  if  [ -z "${S3_ACCESS_KEY}" ]; then
    echo "S3_ACCESS_KEY not found in environment"
  fi

  if  [ -z "${S3_SECRET_KEY}" ]; then
    echo "S3_SECRET_KEY not found in environment"
  fi

  if  [ -z "${S3_BUCKET}" ]; then
    echo "S3_BUCKET not found in environment"
  fi

  if  [ -z "${S3_ENDPOINT}" ]; then
    echo "S3_ENDPOINT not found in environment"
  fi

  if  [ -z "${S3_REGION}" ]; then
    echo "S3_REGION not found in environment"
  fi

  echo "Proceeding without mounting s3 to filesystem"
else
  # Save credentials in a file for s3fs
  echo "${S3_ACCESS_KEY}:${S3_SECRET_KEY}" > /tmp/passwd-s3fs
  chmod 600 /tmp/passwd-s3fs

  # Mount the S3 bucket using s3fs
  s3fs "${S3_BUCKET}" /mnt/s3 \
    -o passwd_file=/tmp/passwd-s3fs \
    -o url="${S3_ENDPOINT}" \
    -o endpoint="${S3_REGION}" \
    -o use_path_request_style \
    -o allow_other \
    -o nonempty

  mkdir -p /mnt/s3/http-jobs
  mkdir -p /http-jobs

  ln -s /mnt/s3/http-jobs /http-jobs

fi

exec "$@"
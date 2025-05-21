FROM flink-with-tools:2.0.0-java21

LABEL maintainer="PSNC"
COPY shared-libs/target/dependency/* /opt/flink/lib/

RUN mkdir -p /mnt/s3
RUN chmod 777 /mnt/s3


COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]

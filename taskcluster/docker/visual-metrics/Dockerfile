# %ARG DOCKER_IMAGE_PARENT
FROM $DOCKER_IMAGE_PARENT
MAINTAINER Gregory Mierzwinski <gmierzwinski@mozilla.com>

USER worker:worker

RUN apt-get update && \
    apt-get install \
      ffmpeg \
      imagemagick \
      pyssim \
      python \
      python-pil \
      python3 \
      python3-pip

WORKDIR /builds/worker

COPY requirements.txt /builds/worker/requirements.txt
RUN pip3 install setuptools==42.0.2
RUN pip3 install --require-hashes -r /builds/worker/requirements.txt && \
    rm /builds/worker/requirements.txt

COPY run-visual-metrics.py /builds/worker/bin/run-visual-metrics.py
RUN chmod +x /builds/worker/bin/run-visual-metrics.py

COPY performance-artifact-schema.json /builds/worker/performance-artifact-schema.json

VOLUME /builds/worker/artifacts/

# run-task expects to run as root
USER root

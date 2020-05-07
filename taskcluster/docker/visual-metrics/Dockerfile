# %ARG DOCKER_IMAGE_PARENT
FROM $DOCKER_IMAGE_PARENT
MAINTAINER Gregory Mierzwinski <gmierzwinski@mozilla.com>

# run-task expects to run as root
USER root

RUN apt-get update -qq && \
    apt-get install -y \
      ffmpeg \
      imagemagick \
      pyssim \
      python \
      python-pil \
      python3.5

# Install python3.5 for compatibility with mozilla-central docker
RUN wget https://bootstrap.pypa.io/get-pip.py
RUN python3.5 get-pip.py

WORKDIR /builds/worker

USER worker:worker

COPY requirements.txt /builds/worker/requirements.txt
RUN python3.5 -m pip install setuptools==46.0.0
RUN python3.5 -m pip install --require-hashes -r /builds/worker/requirements.txt && \
    rm /builds/worker/requirements.txt

COPY similarity.py /builds/worker/bin/similarity.py
COPY run-visual-metrics.py /builds/worker/bin/run-visual-metrics.py
COPY performance-artifact-schema.json /builds/worker/performance-artifact-schema.json

USER root
RUN chmod +x /builds/worker/bin/run-visual-metrics.py

VOLUME /builds/worker/artifacts/

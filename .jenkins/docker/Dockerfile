FROM jenkins/core/there

USER 0


COPY ./contrib/jenkins/configuration $JENKINS_REF_HOME

RUN set -x && \
    chgrp -R 0 $JENKINS_REF_HOME && \
    chmod -R 644 $JENKINS_REF_HOME && \
    chmod -R g+rwX $JENKINS_REF_HOME

ENV JQ_VERSION='1.6'

RUN curl -LJO https://github.com/stedolan/jq/releases/download/jq-1.6/jq-linux64 && \
    mv jq-linux64 /usr/bin/jq && \
    chmod +x /usr/bin/jq 
    
RUN jq --version

USER 1001

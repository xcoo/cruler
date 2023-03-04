FROM clojure:openjdk-11-tools-deps as builder
WORKDIR /cruler
COPY . /cruler
RUN clojure -T:build uber :uber-file cruler.jar

FROM openjdk:11-jre-slim
COPY --from=builder \
     /cruler/cruler.jar \
     /usr/share/cruler/cruler.jar
RUN echo '#!/bin/bash\njava -jar /usr/share/cruler/cruler.jar $@' >> /usr/local/bin/cruler
RUN chmod +x /usr/local/bin/cruler
WORKDIR /cruler
CMD ["cruler"]

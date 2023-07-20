FROM gradle:8.2.1-jdk17

WORKDIR /app

COPY /app .

RUN gradle clean install

CMD ./build/install/app/bin/app
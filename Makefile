.PHONY: repl run test pom jar uberjar install deploy docker clean

repl:
	iced repl -A:dev

run:
	clojure -m antq.core

test:
	clojure -A:dev:test

pom.xml: deps.edn
	clojure -Spom
pom: pom.xml

target/antq-standalone.jar: pom.xml
	clojure -A:depstar -m hf.depstar.uberjar $@ -C -m antq.core
uberjar: clean target/antq-standalone.jar

target/antq.jar: pom.xml
	clojure -A:depstar -m hf.depstar.jar $@
jar: clean target/antq.jar

install: clean target/antq.jar
	clj -R:deploy -m deps-deploy.deps-deploy install target/antq.jar

deploy: clean target/antq.jar
	clj -A:deploy

docker:
	docker build -t uochan/antq .

clean:
	rm -rf target

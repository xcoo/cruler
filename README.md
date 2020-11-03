# Cruler

Cruler is a framework of file format validation.

## What is it

There are many cases you want to implement validation of file format. 
But creating validator is time-consuming because you need to consider input-file format, human-readable error message, result format etc.

Cruler is a validation framework.
Cruler reduces time to create validators and make validator's error more human-readable.

### Features

1. Provide a uniform result format
1. Provide human readable error message
1. Redefine clojure.spec error message
1. Make validators reusable

## Quick Start

Cruler has [sample validators](dev-resources/sample-validator). You can run the validators.

```console
$ clj -Avalidate "dev-resources/sample-validator" -v

Loading config: dev-resources/sample-validator/cruler.edn

Validating :sample-validator.sort/sort

Validating :cruler.validators/start-of-file

Validating :sample-validator.csv-blank/csv-blank

Validating :cruler.validators/blank-line

Validating :cruler.validators/trailing-whitespace

Validating :sample-validator.spec/approval

Validating :sample-validator.reference/approval<->drug

Validating :cruler.validators/end-of-file

Validating :sample-validator.duplication/duplication

Ran 9 validations.
9 passes, 0 failures.
```

Then you should modify a file to be validated.  
For example, you add new line to [description/test.txt](dev-resources/sample-validator/description/test.txt) as follows.

```
Abilify
Bacitracin

Cabergoline
Dabigatran
```

You will get a validation fail message with error preview.

```console
ERROR at :cruler.validators/blank-line validator
Blank line is found at line

dev-resources/sample-validator/description/test.txt
  line: 3
  preview:
-----
Abilify
Bacitracin

Cabergoline
Dabigatran
-----
```

## Usage

### Pre Requirements

+ You already have project which has validator according to the rules of cruler
+ You created `cruler.edn` in the project

In detail, see [ruler of validator](#Rule-of-validator) and [cruler.edn](#cruleredn) section.

### Run Validations

Cruler's basic usage is simple.

```console
$ clj -Avalidate /path/to/validator/project
```

For example, in case you have `sample-validate-project` as follows,

```
├── cruler
└── sample-validate-project
    ├── cruler.edn
    ├── resources
    │   ├── test.txt
    │   └── test.csv
    └── validator
        ├── validator1.clj
        └── validator2.clj
```
then you can run `validator/*.clj`.

```console
$ clj -Avalidate "../sample-validate-project"
...
...

Ran XXX validations.
XXX passes, XXX failures.
```

## Options

```console
$ clj -Avalidate -h

Usage: cruler [<options>] [<directory>]

Options:
  -c, --config CONFIG  Specify a configuration file (default: cruler.edn)
  -v, --verbose        Make cruler verbose during the operation
```

## Rule of validator

A simple validator is as follows.

```clojure
(defmethod validate ::validate-key
  [_ data]
  (let [files (remove #(re-find #"(\A|[^\n]+\n)\z" (:raw-content %)) data)]
    {:errors (map #(select-keys % [:file-path]) files)
     :message "Error message"}))
```

Cruler requires some specifications for validators.
As sample, you can refer [sample-validators](dev-resources/sample-validator/validator/sample_validator).

### defmethod

Cruler defines `(defmulti validate (fn [key _] key))`, and expects validators to be dispatched.
So you should declare validators as `defmulti validate ::key`.

### Args

First arg is not used in validators.
Second arg is `data`. `data` is a array of Map, and the Map conatins key-value as follows.

| key | type | value | description |
|----|----|---|---|
| `:file-path` | string | `(.getPath file)` | Relative path from the project's root dir |
| `:file-type` | keyword | `:csv`, `:text`, or `:yaml` | File type |
| `:raw-content` | string | `(slurp file)` | Raw string of file contents |
| `:parsed-content`  | any | See [cruler.parser](src/cruler/parser.clj) | The parsed data of `:raw-content`. The structure of the data depends on `:file-type`. |

### Return value

Cruler expects validators to return Map. The Map should be as follow.

| key | type | description |
|----|----|---|
| `:errors` | sequence | See [errors](#errors) |
| `:message`  | string | If `:errors` is not empty, the message is shown |

#### :errors

`:errors` is array of Map. The Map should be as follows.

| key | type | description |
|----|----|---|
| `:file-path` | string | Relative path from the project's root dir |
| `:error-value` | string | It should be invalid value |
| `:error-block` | string | A block in `:parsed-content` containing the error location. See [spec.clj](dev-resources/sample-validator/validator/sample_validator/spec.clj) as sample |
| `:error-keys` | sequence | Keys in `:error-block` for indicating the specific error location. |

## cruler.edn

`cruler.edn` is a configuration file of cruler.

```edn
{:validators {
   :validator.namespace/validator-key ["regex of resource file"]}
 :paths ["validator"]
 :deps [[library version]]}
```

`cruler.edn` requires some keys.

| key | require | default | description |
|----|----|----|----|
| `:validators` | true | | Define the validator and resources to be validated |
| `:paths` | false | ["validator"] | Define the classpaths including validator source codes |
| `:deps` | false | nil | Define the dependencies which the project require as library |

See [cruler.edn.sample](dev-resources/cruler.edn.sample) and [sample-validator/cruler.edn](dev-resources/sample-validator/cruler.edn) as samples.

## clojure.spec error messages

`cruler` can make `clojure.spec` error messages human-readable.

In case, you have this yml and define spec.

```yml
- drug: foo
  types:
    - category: A
      # serial: 1234
  comment: A1
```

```clojure
(s/def ::type
  (s/keys :req-un [:sample/serial]
          :opt-un [:sample/category]))
```

You will get this error message.

```console
{:category "A"} - failed: (contains? % :serial) in: [0 :types 0] at: [:types] spec: :sample-validator.spec/type
```

`cruler` will replace this `clojure.spec` error message with readable message like bellow.

```console
error: Missing key: serial
  {:category "A"}
```

### Redefine error message

You can also manually redefine `clojure.spec` error message using `defmsg`.

```clojure
(ns sample
  (:require [cruler.spec-parser :refer [defmsg]]))

(defmsg ::group "Should be A, B or C")
(s/def ::group #{"A" "B" "C"})
```

`cruler` can show error as "Should be A, B or C" if you not satisfy the predicate `::group`.
As sample, you can refer [sample_validator/spec.clj](dev-resources/sample-validator/validator/sample_validator/spec.clj).

## Unit Test & Lint

using [clojure.test](https://clojuredocs.org/clojure.test) and [cljfmt](https://github.com/weavejester/cljfmt), you can run unit test and lint.

```console
$ clj -Atest
$ clj -Alint
```

## License

Copyright 2020 [Xcoo, Inc.](https://xcoo.jp/)

Licensed under the [Apache License, Version 2.0](LICENSE).
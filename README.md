# Codacy Staticcheck

This is the docker engine we use at Codacy to have [Staticcheck](https://github.com/dominikh/go-tools/tree/master/cmd/staticcheck) support.
You can also create a docker to integrate the tool and language of your choice!
Check the **Docs** section for more information.

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c39deb935fd544d4bbc6e833c7f21da3)](https://www.codacy.com/app/machadoit/codacy-staticcheck?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=machadoit/codacy-staticcheck&amp;utm_campaign=Badge_Grade)

## Usage

You can create the docker by doing:

```bash
sbt docker:publishLocal
```

The docker is ran with the following command:

```bash
docker run -it -v $srcDir:/src  <DOCKER_NAME>:<DOCKER_VERSION>
```

## Docs

[Tool Developer Guide](https://support.codacy.com/hc/en-us/articles/207994725-Tool-Developer-Guide)

[Tool Developer Guide - Using Scala](https://support.codacy.com/hc/en-us/articles/207280379-Tool-Developer-Guide-Using-Scala)

## Test

We use the [codacy-plugins-test](https://github.com/codacy/codacy-plugins-test) to test our external tools integration.
You can follow the instructions there to make sure your tool is working as expected.

## Generate Docs

Make sure that you have the ```raw-docs``` for the version that you want to target.
For example, to generate documentation for version ```2017.2.2```, the underlying tool documentation must be available at ```src/main/resources/raw-docks/2017.2.2```.

Using the version ```2017.2.2``` as example, you should copy all the files
from ```https://github.com/dominikh/go-tools/tree/2017.2.2/cmd/staticcheck/docs/checks```
to the ```src/main/resources/raw-docks/2017.2.2```.

As long as the ```raw-docs``` are available, running the command above will generate the ```patterns.json``` and ```description.json```:

```sh
sbt "run-main codacy.staticcheck.DocGenerator <version-of-the-tool>"
```

## What is Codacy

[Codacy](https://www.codacy.com/) is an Automated Code Review Tool that monitors your technical debt, helps you improve your code quality, teaches best practices to your developers, and helps you save time in Code Reviews.

### Among Codacy’s features

- Identify new Static Analysis issues
- Commit and Pull Request Analysis with GitHub, BitBucket/Stash, GitLab (and also direct git repositories)
- Auto-comments on Commits and Pull Requests
- Integrations with Slack, HipChat, Jira, YouTrack
- Track issues in Code Style, Security, Error Proneness, Performance, Unused Code and other categories

Codacy also helps keep track of Code Coverage, Code Duplication, and Code Complexity.

Codacy supports PHP, Python, Ruby, Java, JavaScript, and Scala, among others.

### Free for Open Source

Codacy is free for Open Source projects.

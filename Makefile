JAVA            ?= java
JAVAC           ?= javac
BASH            ?= bash
MAVEN           ?= mvn
SKIP_TESTS      ?= false
BUILD_DOCS      ?= false
REPO_LOC        ?= $(HOME)/.thermostat-build/mvn_repository/
MAVEN_FLAGS     ?= 
XSLTPROC        ?= xsltproc

#
# Do not change anything below
#
REPO_FLAG       = -Dmaven.repo.local=$(REPO_LOC)
GOAL            = package
POM             = pom.xml
ARCH            = $(shell uname -m)
THERMOSTAT_HOME = $(shell pwd)/distribution/target/image

ifeq ($(SKIP_TESTS),true)
	MAVEN_SKIP_TEST = -Dmaven.test.skip=true
endif

ifeq ($(BUILD_DOCS),true)
    MAVEN_JAVADOC = javadoc:aggregate
endif

# Default to just building core
all: core

# At release, also check archetypes
for-release: core verify-archetypes

core:
	$(MAVEN) -f $(POM) $(MAVEN_FLAGS) $(MAVEN_SKIP_TEST) clean $(GOAL) $(MAVEN_JAVADOC)

verify-archetypes: skip-tests-and-install verify-archetype-ext verify-archetype-multimodule

verify-archetype-ext:
	$(BASH) distribution/tools/verify-archetype-ext.sh $(REPO_LOC) $(THERMOSTAT_HOME)

verify-archetype-multimodule:
	$(BASH) distribution/tools/verify-archetype-multimodule.sh $(REPO_LOC) $(THERMOSTAT_HOME)

skip-tests-and-install: clean-repo create-repo-dir
	$(MAVEN) -f $(POM) $(MAVEN_FLAGS) $(REPO_FLAG) -Dmaven.test.skip=true clean install

# 
# Cleaning the repo prevents things like not seeing build failures
# after bundles have been renamed.
core-install: clean-repo create-repo-dir
	$(MAVEN) -f $(POM) $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean install $(MAVEN_JAVADOC)

create-repo-dir:
	mkdir -p $(REPO_LOC)

clean-repo:
	if [ -d $(REPO_LOC) ] ; then \
	  find $(REPO_LOC) -name '*thermostat*' -print0 | xargs -0 rm -rf ; \
	fi

echo-repo:
	echo "Using private Maven repository: $(REPO_LOC)"

plugin-docs: plugin_docs.html

plugin_docs.html:
	$(JAVAC) distribution/tools/MergePluginDocs.java
	$(JAVA) -cp distribution/tools MergePluginDocs > merged-plugin-docs.xml
	$(XSLTPROC) distribution/tools/plugin-docs-html.xslt merged-plugin-docs.xml > $@

# We only have phony targets
.PHONY:	all for-release core verify-archetypes verify-archetype-ext verify-archetype-multimodule skip-tests-and-install core-install create-repo-dir clean-repo echo-repo plugin-docs

/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.github.blindpirate.gogradle.vcs;

import com.github.blindpirate.gogradle.core.dependency.AbstractResolvedDependency;
import com.github.blindpirate.gogradle.core.dependency.NotationDependency;
import com.github.blindpirate.gogradle.core.dependency.resolve.DependencyManager;
import com.github.blindpirate.gogradle.util.Assert;
import com.github.blindpirate.gogradle.util.MapUtils;
import com.github.blindpirate.gogradle.vcs.git.GitResolvedDependency;
import com.github.blindpirate.gogradle.vcs.mercurial.MercurialResolvedDependency;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import static com.github.blindpirate.gogradle.core.dependency.parse.MapNotationParser.NAME_KEY;
import static com.github.blindpirate.gogradle.core.dependency.parse.MapNotationParser.SUBPACKAGES_KEY;
import static com.github.blindpirate.gogradle.core.dependency.parse.MapNotationParser.VCS_KEY;

public abstract class VcsResolvedDependency extends AbstractResolvedDependency {
    private static final int COMMIT_PREFIX_LENGTH = 7;
    private String tag;
    private String url;

    protected VcsResolvedDependency(String name,
                                    String url,
                                    String commitId,
                                    long commitTime) {
        super(name, commitId, commitTime);
        this.url = url;
    }

    public abstract VcsType getVcsType();

    public String getUrl() {
        return url;
    }

    @Override
    protected DependencyManager getInstaller() {
        return getVcsType().getService(DependencyManager.class);
    }

    @Override
    public Map<String, Object> toLockedNotation() {
        Map<String, Object> ret = MapUtils.asMap(
                NAME_KEY, getName(),
                VCS_KEY, getVcsType().getName(),
                GitMercurialNotationDependency.URL_KEY, getUrl(),
                GitMercurialNotationDependency.COMMIT_KEY, getVersion());
        if (!containsAllSubpackages()) {
            ret.put(SUBPACKAGES_KEY, new ArrayList<>(getSubpackages()));
        }
        return ret;
    }

    @Override
    public String formatVersion() {
        if (tag != null) {
            return tag + "(" + getVersion().substring(0, COMMIT_PREFIX_LENGTH) + ")";
        } else {
            return getVersion().substring(0, COMMIT_PREFIX_LENGTH);
        }
    }

    @Override
    public String toString() {
        return getName() + "#" + getVersion();
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        VcsResolvedDependency that = (VcsResolvedDependency) o;
        return Objects.equals(getVcsType(), that.getVcsType())
                && Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, getVersion(), getName(), getVcsType());
    }

    public static GitMercurialResolvedDependencyBuilder builder(VcsType vcsType) {
        return new GitMercurialResolvedDependencyBuilder(vcsType);
    }

    public static final class GitMercurialResolvedDependencyBuilder {
        private VcsType vcsType;
        private GitMercurialNotationDependency notationDependency;
        private String url;
        private String commitId;
        private long commitTime;

        private GitMercurialResolvedDependencyBuilder(VcsType vcsType) {
            this.vcsType = vcsType;
        }

        public GitMercurialResolvedDependencyBuilder withUrl(String repoUrl) {
            this.url = repoUrl;
            return this;
        }

        public GitMercurialResolvedDependencyBuilder withNotationDependency(NotationDependency notationDependency) {
            this.notationDependency = (GitMercurialNotationDependency) notationDependency;
            return this;
        }

        public GitMercurialResolvedDependencyBuilder withCommitId(String commitId) {
            this.commitId = commitId;
            return this;
        }

        public GitMercurialResolvedDependencyBuilder withCommitTime(long commitTime) {
            this.commitTime = commitTime;
            return this;
        }

        public VcsResolvedDependency build() {
            VcsResolvedDependency ret;
            Assert.isTrue(vcsType == VcsType.GIT || vcsType == VcsType.MERCURIAL);
            if (vcsType == VcsType.GIT) {
                ret = new GitResolvedDependency(notationDependency.getName(), url, commitId, commitTime);
            } else {
                ret = new MercurialResolvedDependency(notationDependency.getName(), url, commitId, commitTime);
            }
            ret.tag = notationDependency.getTag();
            ret.setSubpackages(notationDependency.getSubpackages());
            ret.setFirstLevel(notationDependency.isFirstLevel());
            ret.setPackage(notationDependency.getPackage());
            return ret;
        }
    }
}

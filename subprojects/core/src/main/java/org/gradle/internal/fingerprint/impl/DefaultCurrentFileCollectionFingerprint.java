/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.fingerprint.impl;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import org.gradle.api.internal.changedetection.rules.TaskStateChangeVisitor;
import org.gradle.caching.internal.DefaultBuildCacheHasher;
import org.gradle.internal.fingerprint.CurrentFileCollectionFingerprint;
import org.gradle.internal.fingerprint.FileCollectionFingerprint;
import org.gradle.internal.fingerprint.FileFingerprint;
import org.gradle.internal.fingerprint.FingerprintingStrategy;
import org.gradle.internal.fingerprint.HistoricalFileCollectionFingerprint;
import org.gradle.internal.hash.HashCode;
import org.gradle.internal.snapshot.DirectorySnapshot;
import org.gradle.internal.snapshot.FileSystemSnapshot;
import org.gradle.internal.snapshot.PhysicalSnapshot;
import org.gradle.internal.snapshot.PhysicalSnapshotVisitor;

import java.util.Map;

public class DefaultCurrentFileCollectionFingerprint implements CurrentFileCollectionFingerprint {

    private final Map<String, FileFingerprint> fingerprints;
    private final FingerprintCompareStrategy compareStrategy;
    private final FingerprintingStrategy.Identifier identifier;
    private final Iterable<FileSystemSnapshot> roots;
    private final ImmutableMultimap<String, HashCode> rootHashes;
    private HashCode hash;

    public static CurrentFileCollectionFingerprint from(Iterable<FileSystemSnapshot> roots, FingerprintingStrategy strategy) {
        if (Iterables.isEmpty(roots)) {
            return strategy.getIdentifier().getEmptyFingerprint();
        }
        Map<String, FileFingerprint> snapshots = strategy.collectFingerprints(roots);
        if (snapshots.isEmpty()) {
            return strategy.getIdentifier().getEmptyFingerprint();
        }
        return new DefaultCurrentFileCollectionFingerprint(snapshots, strategy.getCompareStrategy(), strategy.getIdentifier(), roots);
    }

    private DefaultCurrentFileCollectionFingerprint(Map<String, FileFingerprint> fingerprints, FingerprintCompareStrategy compareStrategy, FingerprintingStrategy.Identifier identifier, Iterable<FileSystemSnapshot> roots) {
        this.fingerprints = fingerprints;
        this.compareStrategy = compareStrategy;
        this.identifier = identifier;
        this.roots = roots;

        final ImmutableMultimap.Builder<String, HashCode> builder = ImmutableMultimap.builder();
        accept(new PhysicalSnapshotVisitor() {
            @Override
            public boolean preVisitDirectory(DirectorySnapshot directorySnapshot) {
                builder.put(directorySnapshot.getAbsolutePath(), directorySnapshot.getHash());
                return false;
            }

            @Override
            public void visit(PhysicalSnapshot fileSnapshot) {
                builder.put(fileSnapshot.getAbsolutePath(), fileSnapshot.getHash());
            }

            @Override
            public void postVisitDirectory(DirectorySnapshot directorySnapshot) {
            }
        });
        this.rootHashes = builder.build();
    }

    @Override
    public boolean visitChangesSince(FileCollectionFingerprint oldFingerprint, String title, boolean includeAdded, TaskStateChangeVisitor visitor) {
        if (hasSameRootHashes(oldFingerprint)) {
            return true;
        }
        return compareStrategy.visitChangesSince(visitor, getFingerprints(), oldFingerprint.getFingerprints(), title, includeAdded);
    }

    private boolean hasSameRootHashes(FileCollectionFingerprint oldFingerprint) {
        return Iterables.elementsEqual(rootHashes.entries(), oldFingerprint.getRootHashes().entries());
    }

    @Override
    public HashCode getHash() {
        if (hash == null) {
            DefaultBuildCacheHasher hasher = new DefaultBuildCacheHasher();
            compareStrategy.appendToHasher(hasher, fingerprints);
            hash = hasher.hash();
        }
        return hash;
    }

    @Override
    public Map<String, FileFingerprint> getFingerprints() {
        return fingerprints;
    }

    @Override
    public Multimap<String, HashCode> getRootHashes() {
        return rootHashes;
    }

    @Override
    public FingerprintingStrategy.Identifier getStrategyIdentifier() {
        return identifier;
    }

    @Override
    public void accept(PhysicalSnapshotVisitor visitor) {
        if (roots == null) {
            throw new UnsupportedOperationException("Roots not available.");
        }
        for (FileSystemSnapshot root : roots) {
            root.accept(visitor);
        }
    }

    @Override
    public HistoricalFileCollectionFingerprint archive() {
        return new DefaultHistoricalFileCollectionFingerprint(fingerprints, compareStrategy, rootHashes);
    }
}

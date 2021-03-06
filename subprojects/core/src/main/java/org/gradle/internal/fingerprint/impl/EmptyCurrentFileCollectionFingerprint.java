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
import com.google.common.collect.Multimap;
import org.gradle.api.internal.changedetection.rules.TaskStateChangeVisitor;
import org.gradle.internal.fingerprint.CurrentFileCollectionFingerprint;
import org.gradle.internal.fingerprint.FileCollectionFingerprint;
import org.gradle.internal.fingerprint.FileFingerprint;
import org.gradle.internal.fingerprint.FingerprintingStrategy;
import org.gradle.internal.fingerprint.HistoricalFileCollectionFingerprint;
import org.gradle.internal.hash.HashCode;
import org.gradle.internal.hash.Hashing;
import org.gradle.internal.snapshot.PhysicalSnapshotVisitor;

import java.util.Collections;
import java.util.Map;

public class EmptyCurrentFileCollectionFingerprint implements CurrentFileCollectionFingerprint {

    private static final HashCode SIGNATURE = Hashing.md5().hashString(EmptyCurrentFileCollectionFingerprint.class.getName());

    private final FingerprintingStrategy.Identifier identifier;

    public EmptyCurrentFileCollectionFingerprint(FingerprintingStrategy.Identifier identifier) {
        this.identifier = identifier;
    }

    @Override
    public boolean visitChangesSince(FileCollectionFingerprint oldFingerprint, final String title, boolean includeAdded, TaskStateChangeVisitor visitor) {
        return EmptyHistoricalFileCollectionFingerprint.INSTANCE.visitChangesSince(oldFingerprint, title, includeAdded, visitor);
    }

    @Override
    public HashCode getHash() {
        return SIGNATURE;
    }

    @Override
    public Map<String, FileFingerprint> getFingerprints() {
        return Collections.emptyMap();
    }

    @Override
    public void accept(PhysicalSnapshotVisitor visitor) {
    }

    public Multimap<String, HashCode> getRootHashes() {
        return ImmutableMultimap.of();
    }

    @Override
    public FingerprintingStrategy.Identifier getStrategyIdentifier() {
        return identifier;
    }

    @Override
    public HistoricalFileCollectionFingerprint archive() {
        return EmptyHistoricalFileCollectionFingerprint.INSTANCE;
    }

    @Override
    public String toString() {
        return "EMPTY{" + identifier.name() + "}";
    }
}

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

import com.google.common.collect.ImmutableMap;
import org.gradle.internal.file.FileType;
import org.gradle.internal.fingerprint.FileFingerprint;
import org.gradle.internal.fingerprint.FingerprintingStrategy;
import org.gradle.internal.snapshot.DirectorySnapshot;
import org.gradle.internal.snapshot.FileSystemSnapshot;
import org.gradle.internal.snapshot.PhysicalSnapshot;
import org.gradle.internal.snapshot.PhysicalSnapshotVisitor;

import java.util.HashSet;
import java.util.Map;

/**
 * Fingerprint files without path or content normalization.
 */
public enum AbsolutePathFingerprintingStrategy implements FingerprintingStrategy {
    INCLUDE_MISSING(true),
    IGNORE_MISSING(false);

    private final boolean includeMissing;

    AbsolutePathFingerprintingStrategy(boolean includeMissing) {
        this.includeMissing = includeMissing;
    }

    @Override
    public Map<String, FileFingerprint> collectFingerprints(Iterable<FileSystemSnapshot> roots) {
        final ImmutableMap.Builder<String, FileFingerprint> builder = ImmutableMap.builder();
        final HashSet<String> processedEntries = new HashSet<String>();
        for (FileSystemSnapshot root : roots) {
            root.accept(new PhysicalSnapshotVisitor() {

                @Override
                public boolean preVisitDirectory(DirectorySnapshot directorySnapshot) {
                    String absolutePath = directorySnapshot.getAbsolutePath();
                    if (processedEntries.add(absolutePath)) {
                        builder.put(absolutePath, new DefaultFileFingerprint(directorySnapshot.getAbsolutePath(), directorySnapshot));
                    }
                    return true;
                }

                @Override
                public void visit(PhysicalSnapshot fileSnapshot) {
                    if (!includeMissing && fileSnapshot.getType() == FileType.Missing) {
                        return;
                    }
                    String absolutePath = fileSnapshot.getAbsolutePath();
                    if (processedEntries.add(absolutePath)) {
                        builder.put(absolutePath, new DefaultFileFingerprint(fileSnapshot.getAbsolutePath(), fileSnapshot));
                    }
                }

                @Override
                public void postVisitDirectory(DirectorySnapshot directorySnapshot) {
                }

            });
        }
        return builder.build();
    }

    @Override
    public FingerprintCompareStrategy getCompareStrategy() {
        return FingerprintCompareStrategy.ABSOLUTE;
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.ABSOLUTE_PATH;
    }

}

/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.tracker.validation.hooks;

import static org.hisp.dhis.tracker.TrackerType.TRACKED_ENTITY;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1048;
import static org.hisp.dhis.tracker.validation.hooks.AssertValidationErrorReporter.hasTrackerError;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.validation.ValidationErrorReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Enrico Colasante
 */
class TrackedEntityCheckUidValidatorTest
{

    private static final String INVALID_UID = "InvalidUID";

    private TrackedEntityPreCheckUidValidator validator;

    private TrackerBundle bundle;

    private ValidationErrorReporter reporter;

    @BeforeEach
    void setUp()
    {
        TrackerPreheat preheat = new TrackerPreheat();
        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
        preheat.setIdSchemes( idSchemes );
        reporter = new ValidationErrorReporter( idSchemes );
        bundle = TrackerBundle.builder().preheat( preheat ).build();

        validator = new TrackedEntityPreCheckUidValidator();
    }

    @Test
    void verifyTrackedEntityValidationSuccess()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .build();

        validator.validate( reporter, bundle, trackedEntity );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyTrackedEntityWithInvalidUidFails()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( INVALID_UID )
            .orgUnit( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .build();

        validator.validate( reporter, bundle, trackedEntity );

        hasTrackerError( reporter, E1048, TRACKED_ENTITY, trackedEntity.getUid() );
    }
}
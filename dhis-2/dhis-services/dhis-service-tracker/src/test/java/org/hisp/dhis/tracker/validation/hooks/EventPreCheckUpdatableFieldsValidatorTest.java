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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1128;
import static org.hisp.dhis.tracker.validation.hooks.AssertValidationErrorReporter.hasTrackerError;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.validation.ValidationErrorReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Enrico Colasante
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class EventPreCheckUpdatableFieldsValidatorTest
{

    private final static String TRACKED_ENTITY_TYPE_ID = "TrackedEntityTypeId";

    private final static String PROGRAM_ID = "ProgramId";

    private final static String PROGRAM_STAGE_ID = "ProgramStageId";

    private final static String TRACKED_ENTITY_ID = "TrackedEntityId";

    private final static String ENROLLMENT_ID = "EnrollmentId";

    private final static String EVENT_ID = "EventId";

    private EventPreCheckUpdatableFieldsValidator validator;

    @Mock
    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    private ValidationErrorReporter reporter;

    @BeforeEach
    public void setUp()
    {
        validator = new EventPreCheckUpdatableFieldsValidator();

        when( bundle.getImportStrategy() ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );

        when( bundle.getStrategy( any( TrackedEntity.class ) ) ).thenReturn( TrackerImportStrategy.UPDATE );
        when( bundle.getStrategy( any( Enrollment.class ) ) ).thenReturn( TrackerImportStrategy.UPDATE );
        when( bundle.getStrategy( any( Event.class ) ) ).thenReturn( TrackerImportStrategy.UPDATE );

        when( preheat.getTrackedEntity( TRACKED_ENTITY_ID ) ).thenReturn( trackedEntityInstance() );
        when( preheat.getEnrollment( ENROLLMENT_ID ) ).thenReturn( programInstance() );
        when( preheat.getEvent( EVENT_ID ) ).thenReturn( programStageInstance() );

        when( bundle.getPreheat() ).thenReturn( preheat );

        reporter = new ValidationErrorReporter( TrackerIdSchemeParams.builder().build() );
    }

    @Test
    void verifyEventValidationSuccess()
    {
        Event event = validEvent();

        validator.validate( reporter, bundle, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEventValidationFailsWhenUpdateProgramStage()
    {
        Event event = validEvent();
        event.setProgramStage( MetadataIdentifier.ofUid( "NewProgramStageId" ) );

        validator.validate( reporter, bundle, event );

        hasTrackerError( reporter, E1128, EVENT, event.getUid() );
        assertThat( reporter.getErrors().get( 0 ).getErrorMessage(), containsString( "programStage" ) );
    }

    @Test
    void verifyEventValidationFailsWhenUpdateEnrollment()
    {
        Event event = validEvent();
        event.setEnrollment( "NewEnrollmentId" );

        validator.validate( reporter, bundle, event );

        hasTrackerError( reporter, E1128, EVENT, event.getUid() );
        assertThat( reporter.getErrors().get( 0 ).getErrorMessage(), containsString( "enrollment" ) );
    }

    private Event validEvent()
    {
        return Event.builder()
            .event( EVENT_ID )
            .programStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_ID ) )
            .enrollment( ENROLLMENT_ID )
            .build();
    }

    private TrackedEntityInstance trackedEntityInstance()
    {
        TrackedEntityType trackedEntityType = new TrackedEntityType();
        trackedEntityType.setUid( TRACKED_ENTITY_TYPE_ID );

        TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
        trackedEntityInstance.setUid( TRACKED_ENTITY_ID );
        trackedEntityInstance.setTrackedEntityType( trackedEntityType );
        return trackedEntityInstance;
    }

    private ProgramInstance programInstance()
    {
        Program program = new Program();
        program.setUid( PROGRAM_ID );

        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( ENROLLMENT_ID );
        programInstance.setProgram( program );
        programInstance.setEntityInstance( trackedEntityInstance() );
        return programInstance;
    }

    private ProgramStageInstance programStageInstance()
    {
        ProgramStage programStage = new ProgramStage();
        programStage.setUid( PROGRAM_STAGE_ID );

        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        programStageInstance.setUid( EVENT_ID );
        programStageInstance.setProgramInstance( programInstance() );
        programStageInstance.setProgramStage( programStage );
        return programStageInstance;
    }
}
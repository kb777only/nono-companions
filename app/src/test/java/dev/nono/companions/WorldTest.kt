package dev.nono.companions

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random
import java.io.StringReader
import java.io.StringWriter

class WorldTest {
    private fun world()=World(Random(7)).apply { resize(400f,700f,100f,150f,0); pets.forEach { it.state=State.IDLE; it.y=550f; it.motion.grounded=true }; pets[0].x=90f; pets[1].x=180f }
    @Test fun snackTransfersThenReconcilesForEitherThief() {
        Who.entries.forEach { owner ->
            val w=world(); assertTrue(w.start(Kind.SNACK,owner,1000)); var stolen=false
            for(t in 1000L..19000L step 80) { w.tick(t); if(w.prop?.owner==owner.partner()) stolen=true }
            assertTrue(stolen); assertNull(w.interaction); assertNull(w.prop); assertTrue(w.bond>25f)
            assertTrue(w.pets.all { it.needs.hunger<45f })
        }
    }
    @Test fun draggingEitherParticipantCancelsEveryStage() {
        for(kind in Kind.entries) for(who in Who.entries) for(delay in listOf(0L,3000L,6000L)) {
            val w=world(); w.start(kind,Who.HUSBAND,1000)
            for(t in 1000L..(1000+delay) step 80) w.tick(t)
            w.command(Command.Drag(who,900f,-80f),1100+delay)
            assertNull(w.interaction); assertNull(w.prop); assertNull(w.bubble)
            assertEquals(State.DRAGGED,w.pet(who).state); assertEquals(300f,w.pet(who).x); assertEquals(0f,w.pet(who).y)
            w.command(Command.Release(who),1200+delay); assertEquals(State.RECOVERING,w.pet(who).state)
            for(t in (1200+delay)..(10000+delay) step 32) w.tick(t)
            assertTrue(w.pet(who).motion.grounded); assertTrue(w.pet(who).state!=State.DRAGGED)
        }
    }
    @Test fun propLossAbortsInsteadOfLeavingPartnerWaiting() {
        val w=world(); w.start(Kind.SNACK,Who.WIFE,1000); w.prop=null; w.tick(1100)
        assertNull(w.interaction); assertTrue(w.pets.all { it.state==State.RECOVERING })
    }
    @Test fun foldClampsBothAndCancelsConversation() {
        val w=world(); w.start(Kind.AFFECTION,Who.WIFE,1000); w.resize(160f,180f,50f,70f,1100)
        assertNull(w.interaction); assertNull(w.bubble)
        assertTrue(w.pets.all { it.x in 0f..110f && it.y in 0f..110f })
    }
    @Test fun dinoHasExactCoordinatedTurnsAndCooldown() {
        val w=world(); w.start(Kind.DINO,Who.HUSBAND,1000); assertEquals("dinsoauurr...",w.bubble?.text)
        w.tick(4300); assertEquals("rawrrr",w.bubble?.text); assertEquals(Who.WIFE,w.bubble?.who)
        w.tick(7900); assertNull(w.interaction); assertFalse(w.start(Kind.DINO,Who.WIFE,8000))
    }
    @Test fun allSequencesFinishWithoutOrphanProps() {
        Kind.entries.forEach { kind -> val w=world(); w.start(kind,Who.WIFE,1000); for(t in 1000L..19000L step 80) { w.tick(t); if(w.interaction==null) break }; assertNull("$kind",w.interaction); assertNull(w.prop); assertTrue(w.bond>25) }
    }
    @Test fun needsRoundTripAndElapsedTimeAreBounded() {
        val w=world(); w.pets[0].needs.hunger=97f; w.bond=78f
        val out=StringWriter(); MoodCodec.write(w,1000,out)
        val restored=world(); MoodCodec.read(restored,1000+365L*86400000,StringReader(out.toString()))
        assertEquals(100f,restored.pets[0].needs.hunger); assertEquals(78f,restored.bond)
        assertTrue(restored.pets.all { it.needs.energy in 0f..100f })
        MoodCodec.read(restored,0,StringReader("version=1\nat=999999\nbond=NaN\nHUSBAND.0=Infinity"))
        assertEquals(25f,restored.bond); assertEquals(45f,restored.pets[0].needs.hunger)
    }
    @Test fun lowEnergyProducesQuietRestAndNoConcurrentInteraction() {
        val w=world(); w.pets.forEach { it.needs.energy=0f }; w.tick(13000)
        assertTrue(w.pets.any { it.state==State.RESTING }); assertNull(w.interaction)
        w.command(Command.Drag(Who.WIFE,10f,10f),14000); assertFalse(w.start(Kind.DINO,Who.HUSBAND,15000))
    }
    @Test fun contextChatterIsRateLimitedAndDoesNotInterruptCouple() {
        val w=world(); w.command(Command.Context(ContextSignal.READING),1000); val first=w.bubble
        w.command(Command.Context(ContextSignal.GAME),5000); assertEquals(first,w.bubble)
        w.start(Kind.SNACK,Who.WIFE,7000); w.command(Command.Context(ContextSignal.MEDIA),200000)
        assertEquals(Kind.SNACK,w.interaction?.kind)
    }
    @Test fun playbackInterruptsImmediately() {
        val p=AnimationPlayer(); p.frame(Pose.WALK,1000); assertEquals(2,p.frame(Pose.WALK,1300)); assertEquals(23,p.frame(Pose.SURPRISE,1301)); assertEquals(4,p.frame(Pose.IDLE,1302))
    }
    @Test fun displayChangeAndScreenOffCannotLeaveDraggingStuck() {
        val w=world(); w.command(Command.Drag(Who.HUSBAND,10f,10f),1000)
        w.resize(200f,300f,60f,90f,1100); assertEquals(State.RECOVERING,w.pet(Who.HUSBAND).state)
        w.command(Command.Drag(Who.WIFE,10f,10f),2000); w.interrupt(2100)
        w.tick(3100); assertTrue(w.pets.none { it.state==State.DRAGGED })
    }
    @Test fun upwardDragFallsToFloorAndSettles() {
        val w=world(); w.tick(1000); w.command(Command.Drag(Who.HUSBAND,90f,40f),1100); w.command(Command.Release(Who.HUSBAND),1200)
        assertEquals(Pose.FALL,w.pose(Who.HUSBAND,1200))
        w.tick(1250); assertTrue(w.pet(Who.HUSBAND).y>40f)
        for(t in 1280L..5000L step 32) w.tick(t)
        assertEquals(550f,w.pet(Who.HUSBAND).y,.5f); assertTrue(w.pet(Who.HUSBAND).motion.grounded)
    }
    @Test fun rotationChangesDownAndInterruptsCouple() {
        val w=world(); w.tick(1000); w.start(Kind.SNACK,Who.HUSBAND,1100); w.gravity(-1f,0f,1200)
        assertNull(w.interaction); assertNull(w.prop)
        for(t in 1200L..5000L step 32) w.tick(t)
        assertEquals(0f,w.pet(Who.HUSBAND).x,.5f)
        w.gravity(0f,-1f,5100)
        for(t in 5120L..9000L step 32) w.tick(t)
        assertEquals(0f,w.pet(Who.HUSBAND).y,.5f)
    }
    @Test fun screenRotationMapsSensorAxesAndFlatFallback() {
        assertEquals(GravityVector(0f,1f),GravityMapping.screen(0f,0f,0))
        val down=GravityMapping.screen(0f,9.8f,0); assertEquals(1f,down.y,.001f)
        val landscape=GravityMapping.screen(9.8f,0f,1); assertEquals(1f,landscape.y,.001f)
        val upside=GravityMapping.screen(0f,-9.8f,2); assertEquals(1f,upside.y,.001f)
    }
    @Test fun largeTimeStepCannotTunnelThroughBoundaries() {
        val w=world(); val p=w.pet(Who.HUSBAND); p.y=20f; p.motion.vy=100000f
        w.physics.step(p,400f,700f,100f,150f,100f,1000)
        assertTrue(p.y in 0f..550f); assertTrue(p.motion.vy.isFinite())
    }
    @Test fun draggingDoesNotIntegrateGravity() {
        val w=world(); val p=w.pet(Who.WIFE); w.command(Command.Drag(Who.WIFE,50f,80f),1000)
        w.physics.step(p,400f,700f,100f,150f,1f,2000)
        assertEquals(50f,p.x); assertEquals(80f,p.y)
    }
    @Test fun snackAndReleaseDialogueIsFrench() {
        val w=world(); w.start(Kind.SNACK,Who.HUSBAND,1000)
        assertEquals("Enfin un goûter !",w.bubble?.text)
        w.command(Command.Drag(Who.WIFE,50f,80f),5000); w.command(Command.Release(Who.WIFE),6000)
        assertEquals("Rattrape-moi !",w.bubble?.text)
    }
    @Test fun commandBoundaryRejectsNonFiniteMotion() {
        val w=world(); val x=w.pet(Who.HUSBAND).x
        w.command(Command.Drag(Who.HUSBAND,Float.NaN,20f),1000); assertEquals(x,w.pet(Who.HUSBAND).x)
        w.command(Command.Drag(Who.HUSBAND,50f,20f),2000); w.command(Command.Release(Who.HUSBAND,Float.POSITIVE_INFINITY,Float.NaN),2100)
        assertTrue(w.pet(Who.HUSBAND).motion.vx.isFinite()); assertTrue(w.pet(Who.HUSBAND).motion.vy.isFinite())
    }
}

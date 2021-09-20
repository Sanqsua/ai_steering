package s0559587;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Polygon;

import org.lwjgl.util.vector.Vector2f;

import lenz.htw.ai4g.ai.AI;
import lenz.htw.ai4g.ai.DriverAction;
import lenz.htw.ai4g.ai.Info;
import lenz.htw.ai4g.track.Track;

public class MeineSuperAI extends AI {

	float tolerance = 0.06f;
	float posVelocity;
	float rotVelocity;
	float brakeAngle = 1f;
	float myDesiredAngularVelocity;
	float myDesiredTime = 0.1f;
	float targetOrient;
	float checkPointDistance = 1;
	float checkPointDistanceAhead = 40;
	float avoidanceStrength = 1f;

	// Arrive
	int zielRadius = 7;
	int abbremsradius = 50;
	float distance;
	int wunschzeit = 1;

	// Align

	boolean swing = false;

	// CheckPointGatheredBehavior
	boolean didHeJustStart = true;
	boolean checkPointDone = false;
	int checkPointsGathered = 0;
	int downtime = 0;

	// Obstacles
	private Vector2f ahead;
	private Vector2f ahead2;
	private Vector2f ahead3;
	private Vector2f aheadCloser;
	private Vector2f aheadCloser2;
	private Vector2f aheadCloser3;
	private Vector2f carPos;
	private Vector2f targetPos;
	private Vector2f[] aheadArrVelo = new Vector2f[60];
	private Vector2f[] aheadArrWests = new Vector2f[60];
	private Vector2f[] aheadArrEasts = new Vector2f[60];
	private static final int OBS_RADIUS = 10;
	private float obsDistance;

	private boolean standing = false;
	private int standingtimer = 0;
	
	//Graph G;
	//static final CELL_SIZE = 10;  //eine Zelle 10x10 betrachtet man als ein Knoten d.h bei unserer Strecke von 1000x1000 = 100
	//-> vorteil Kantenlänge immer 10 wenn von mitte zu mitte

	public MeineSuperAI(Info info) {
		super(info);
//		enlistForTournament(559587, 558281);
		
		// hier irgendwas -> Graph erstellen
		//G = tueWasSinnvollesMit(info.getTrack().getObstacles());
		//weg = G.wegeSuche(von,nach);
		
		//Variante 1:
		
		//info.getTrack().width()/CELL_SIZE
		//info.getTrack().height()/CELL_SIZE -> Ergebnis = min. Anzahl im Array
		
		//frei, also scneidet ein Hinderniss die Zelle?
		//info.getTrack().getObstacles()[0].intersects(x,y, CELL_SIZE, CELL_SIZE)
		
		//boolean Array füllen mit den Zellen die frei sind -> bei doDebugStuff prüfbar mit GL_Quads
		//boolean freeSpace[x][y] -> for X, for Y -> if(freeSpace[x][y]) -> glColor(1,0,0) else glColor(1,1,0) -> glBegin(GL_QUADS) -> glVertex2f(info.getX(), infogetY()
		
		//Variante 3: Reflexecken identifizieren
		
		//info.getTrack().getObstacles()[0].xpoints[0]
		//info.getTrack().getObstacles()[0].ypoints[0]
		//List erstellen, durchgehen -> Paare bilden
		//für alle Paare von Ref.ecken
				//Verbindungen im Freespace? -> Strecke schneidet Polygon?
		//Klasse Area kann helfen
		//java.awt.geom.Area a = new Area(info.getTrack().getObstacles()[0]); -> gibt auch für Area intersects methoden, area class ist aber bruteforce
		
		
	}

	@Override
	public String getName() {
		return "SANIC";
	}

	float neededRotation;

	boolean neededRotationGreaterThanBrakeAngle = false;
	boolean toleranceBool = false;
	boolean neededRotationTinierThanBrakeAngle = false;

	boolean oneEighty = false;
	boolean carOritentationIsPositive = false;
	int oneEightyInt = 0;
	Polygon obs;
	int test;
	
	//Weg weg;
	//int lastCheckpointX, lastCheckpointY;
	
	@Override
	public DriverAction update(boolean wasResetAfterCollision) {
		
		//Wegesuche
		//if(checkpointErreicht)  oder if (weg.binAmEnde()) || wasResetAfterCollision
		//weg = G.wegeSuche(von,nach);

		//if(info.getCurrentCheckPoint().x != lastCheckPoint || info.getCurrentCheckpoint().y != lastCheckPointY)
		
		
		// Avoid Obstacles
		Track track = info.getTrack();
		track.getObstacles(); // Hindernisse - nächste Übung
		track.getHeight();
		track.getWidth();
		Polygon[] obstacles = track.getObstacles(); // Hindernisfläche

		// obs.npoints; //Anzahl Punkte des Hindernisses
		// A = obs.xpoints[0], obs.ypoints[0];
		// B = obs.xpoints[1], obs.ypoints[1];
		// Erstelle Strecke von A nach B
		// Erstelle Richtungsvektor
		// Berechne Schnittpunkte der beiden obigen, prüfe Abstand

		carPos = new Vector2f(info.getX(), info.getY());
		targetPos = new Vector2f(info.getCurrentCheckpoint().x, info.getCurrentCheckpoint().y);
		// Richtung: Ziel - Start
		Vector2f direction = new Vector2f(targetPos.x - carPos.x, targetPos.y - carPos.y);
		direction.normalise(direction);

		// Orientierung zw. Auto und Ziel
		float carOrient = info.getOrientation();

		targetOrient = (float) Math.atan2(direction.y, direction.x);
		neededRotation = targetOrient - carOrient;

		// Arrive

		distance = (float) Math.sqrt(Math.pow(carPos.x - targetPos.x, 2) + Math.pow(carPos.y - targetPos.y, 2));

		float wunschgeschwindigkeit;

		if (distance < zielRadius) {
			posVelocity = 0;
		}
		if (distance < abbremsradius) {
			wunschgeschwindigkeit = (distance) * info.getMaxVelocity() / abbremsradius;
		} else {
			wunschgeschwindigkeit = info.getMaxVelocity();
		}
		posVelocity = (wunschgeschwindigkeit - info.getVelocity().length()) / 0.05f;
		if (posVelocity > 28) {
			posVelocity = 28;
		}
		
		// Align

		/*
		 * Winkel zw. Orientierung < Toleranz --bereits angekommne, fertig! Winkel zw.
		 * Orientierungen < Abbremswinkel -- Wunschdrehgeschwindigkeit = (ZielOrient
		 * -StartOrient) * max. Drehgeschwindigkeit/Abbremswinkel -- sonstr
		 * Wunschdrehgeschwindigkeit = max. Drehgeschwindigkeit
		 * (Wunschdrehgeschwindikeit - aktuelle Drehgeschwindigkeit)/ Wunschzeit ->
		 * gegen Maximalbeschleunigung clippen
		 */

		if (Math.abs(neededRotation) < tolerance) {
			neededRotationState(neededRotation);
			rotVelocity = 0;
		}

		if (neededRotation < brakeAngle) {
			neededRotationState(neededRotation);
			myDesiredAngularVelocity = (neededRotation) * info.getMaxAbsoluteAngularVelocity();
		} else {
			if (neededRotation < Math.PI) {
				myDesiredAngularVelocity = info.getMaxAbsoluteAngularVelocity();
			} else {
				myDesiredAngularVelocity = -info.getMaxAbsoluteAngularVelocity();
			}
		}

		if (neededRotation < -4.4) {
			myDesiredAngularVelocity = info.getMaxAbsoluteAngularVelocity();
		}

		// Obstacle Avoidance

		for (int i = 0; i < aheadArrWests.length; i++) {
			aheadArrWests[i] = new Vector2f(
					(float) (carPos.x + Math.cos(info.getOrientation() + 0.3f) * ((checkPointDistance * i))),
					(float) (carPos.y + Math.sin(info.getOrientation() + 0.3f) * ((checkPointDistance * i))));
			aheadArrEasts[i] = new Vector2f(
					(float) (carPos.x + Math.cos(info.getOrientation() - 0.3f) * ((checkPointDistance * i))),
					(float) (carPos.y + Math.sin(info.getOrientation() - 0.3f) * ((checkPointDistance * i))));
			aheadArrVelo[i] = new Vector2f(
					(float) (carPos.x + Math.cos(info.getOrientation()) * (checkPointDistance * i * 1.1f)),
					(float) (carPos.y + Math.sin(info.getOrientation()) * (checkPointDistance * i * 1.1f)));
		}
		;

		// aheadCloser = new Vector2f((float) carPos.x * normalizedVelocity.getX() *
		// checkPointDistance * 5,
		// (float) carPos.getY() * normalizedVelocity.getY() * checkPointDistance * 5);
		// aheadCloser2 = new Vector2f((float) (carPos.x +
		// Math.cos(info.getOrientation() + 0.2) * checkPointDistance /2),
		// (float) (carPos.y + Math.sin(info.getOrientation() + 0.2) *
		// checkPointDistance /2 ));
		// ahead3 = new Vector2f((float) (carPos.x + Math.cos(info.getOrientation() -
		// 0.2) * checkPointDistance),
		// (float) (carPos.y + Math.sin(info.getOrientation() - 0.2) *
		// checkPointDistance));
		// aheadCloser = new Vector2f(carPos.x + normalizedVelocity.getX() *
		// (checkPointDistance / 2),
		// carPos.y + normalizedVelocity.getY() * (checkPointDistanceAhead / 2));
		// aheadCloser3 = new Vector2f((float) (carPos.x +
		// Math.cos(info.getOrientation() - 0.2) * checkPointDistance / 2),
		// (float) (carPos.y + Math.sin(info.getOrientation() - 0.2) *
		// checkPointDistance / 2));

		// if (obstacles[2].contains(ahead.x, ahead.y)) {
		//// System.out.println("collision");
		// myDesiredAngularVelocity = info.getMaxAbsoluteAngularVelocity() *
		// (neededRotation - avoidanceStrength/2);
		// wunschgeschwindigkeit = distance * info.getMaxVelocity() / 20;
		// posVelocity = ( wunschgeschwindigkeit - info.getVelocity().length()) / 0.05f;
		//
		// // First Case: mittlerer und rechter Strahl treffen das Ziel -> Action: links
		// abbiegen
		//
		// if (obstacles[2].contains(ahead.x, ahead.y) &&
		// obstacles[2].contains(ahead2.x, ahead2.y)) {
		// myDesiredAngularVelocity = info.getMaxAbsoluteAngularVelocity() *
		// (neededRotation - avoidanceStrength);
		//
		// // First Case 2.0 : mittlerer und rechter kurzer Strahl treffen das Ziel ->
		// Action: scharf links abbiegen
		//
		// if (obstacles[2].contains(.x, ahead.y) &&
		// obstacles[2].contains(aheadCloser2.x, aheadCloser2.y)) {
		// myDesiredAngularVelocity = info.getMaxAbsoluteAngularVelocity() *
		// (neededRotation - avoidanceStrength * 1.5f);
		// }
		//
		// // Second Case: mittlerer und linker Strahl treffen das Ziel -> Action:
		// rechts abbiegen
		//
		// } else if (obstacles[2].contains(ahead.x, ahead.y) &&
		// obstacles[2].contains(ahead3.x, ahead3.y)) {
		// myDesiredAngularVelocity = info.getMaxAbsoluteAngularVelocity() *
		// (neededRotation + avoidanceStrength);
		//
		// // Second Case: mittlerer und linker kurzer Strahl treffen das Ziel ->
		// Action: rechts abbiegen
		//
		// if (obstacles[2].contains(ahead.x, ahead.y) &&
		// obstacles[2].contains(aheadCloser3.x, aheadCloser3.y)) {
		// myDesiredAngularVelocity = info.getMaxAbsoluteAngularVelocity() *
		// (neededRotation + avoidanceStrength * 1.5f);
		// }
		// }
		//
		// }

		for (int i = 1; i < track.getObstacles().length; i++) {

			if (i > 1) {

				for (int j = aheadArrWests.length - 1; j != 1; j--) {
					boolean checkWest = obstacles[i].contains(aheadArrWests[j].getX(), aheadArrWests[j].getY())
							&& distance > 30;
					boolean checkEast = obstacles[i].contains(aheadArrEasts[j].getX(), aheadArrEasts[j].getY())
							&& distance > 30;
					boolean frontCheck = obstacles[i].contains(aheadArrVelo[j].getX(), aheadArrVelo[j].getY())
							&& distance > 30;
					if (frontCheck) {
						posVelocity = 0.1f * aheadArrVelo.length / j;
					}
					if (checkWest) {
						posVelocity = 0.2f * aheadArrVelo.length / j;
						myDesiredAngularVelocity = info.getMaxAbsoluteAngularVelocity()
								* (neededRotation - avoidanceStrength * (aheadArrWests.length * 1 / j));

					} else if (checkEast) {
						posVelocity = 0.2f * aheadArrVelo.length / j;

						myDesiredAngularVelocity = info.getMaxAbsoluteAngularVelocity()
								* (neededRotation + avoidanceStrength * (aheadArrWests.length * 1 / j));

					}
				}
			}

		}

		rotVelocity = (myDesiredAngularVelocity - info.getAngularVelocity()) / myDesiredTime;
		if (rotVelocity > 1.5f) {
			rotVelocity = 1.5f;
		}

		checkPointGatheredBehavior();
		isStanding(standing, standingtimer);
		return new DriverAction(posVelocity, rotVelocity);

	}

	private void checkPointGatheredBehavior() {
		if (distance <= 10) {
			checkPointDone = true;
		}
		if (checkPointDone) {
			posVelocity = 0.001f;

			downtime++;
			if (downtime == 60) {

				downtime = 0;
				checkPointDone = false;
			}
		}

		if (didHeJustStart) {
			downtime++;
			posVelocity = 0.01f;
			if (downtime == 60) {
				downtime = 0;
				didHeJustStart = false;
			}
		}
		checkPointsGathered++;
		if (checkPointsGathered == 9) {
			posVelocity = info.getMaxVelocity();
		}
	}

	int counter = 0;
	static final int FRAMES = 30;

	@Override
	public String getTextureResourceName() {
		return "/s0559587/car.png";
	}

	@Override
	public void doDebugStuff() {
		// glBegin(GL_LINES);
		// glColor3f(1,1,1);
		// glVertex2f(info.getX(), info.getY());
		// glVertex2d(ahead.getX(), ahead.getY());
		// glEnd();
		//
		// glBegin(GL_LINES);
		// glColor3f(1,0,0);
		// glVertex2f(info.getX(), info.getY());
		// glVertex2d(aheadCloser.getX(), aheadCloser.getY());
		// glEnd();

		// glBegin(GL_LINES);
		// glColor3f(0, 0, 1);
		// glVertex2f(info.getX(), info.getY());
		// glVertex2d(ahead2.getX(), ahead2.getY());
		// glEnd();
		//
		// glBegin(GL_LINES);
		// glColor3f(0, 1, 0);
		// glVertex2f(info.getX(), info.getY());
		// glVertex2d(ahead3.getX(), ahead3.getY());
		// glEnd();
		//
		// for (int i = 0; i < obs.xpoints.length-1; i++) {
		// glColor3f(0.1f*i, i*0.2f, i*.3f);
		// // glVertex2f(info.getX(), info.getY());
		// // glVertex2d(info.getCurrentCheckpoint().getX(),
		// // info.getCurrentCheckpoint().getY());
		// glVertex2d(obs.xpoints[i], obs.ypoints[i]);
		// glVertex2d(obs.xpoints[i+1], obs.ypoints[i+1]);
		// }

		glBegin(GL_LINES);
		glVertex2f(info.getX(), info.getY());

		glEnd();
		for (int i = 0; i < aheadArrEasts.length; i++) {
			glBegin(GL_LINES);
			glColor3f(i, i, i);
			glVertex2d(info.getX(), info.getY());
			glVertex2f(info.getX() + aheadArrEasts[i].getX(), info.getY() + aheadArrEasts[i].getY());
			glEnd();

			// glVertex2f(aheadArrVelo[i].getX(), aheadArrVelo[i].getY());
		}

		if (counter == FRAMES) {
			counter = 0;
			lookInWhatState();

			// System.out.println(distance);

			System.out.printf("neededRotation %f \n", neededRotation);
			System.out.println(info.getVelocity().length());
			System.out.println(info.getAngularVelocity());

			// for (int i = 0; i < obs.xpoints.length-1; i++) {
			// Vector2f a = new Vector2f(obs.xpoints[i],obs.ypoints[i]);
			// Vector2f b = new Vector2f(obs.xpoints[i+1],obs.ypoints[i+1]);
			// System.out.println(a);
			// System.out.println(b);
			// }

		}
		counter++;
	}

	private void isStanding(boolean standing, int standingTimer) {
		if (info.getVelocity().length() < 1.5f) {
			standingTimer++;
		} else {
			standingTimer = 0;
		}
		if (standingTimer == 120)
			standing = true;
		if (standing)
			posVelocity = info.getMaxVelocity();
	}

	private void neededRotationState(float neededRotation) {
		if (Math.abs(neededRotation) < tolerance) {
			toleranceBool = true;
			neededRotationGreaterThanBrakeAngle = false;
			neededRotationTinierThanBrakeAngle = false;
		} else if (neededRotation < brakeAngle) {
			toleranceBool = false;
			neededRotationGreaterThanBrakeAngle = false;
			neededRotationTinierThanBrakeAngle = true;
		} else {
			toleranceBool = false;
			neededRotationGreaterThanBrakeAngle = true;
			neededRotationTinierThanBrakeAngle = false;
		}
	}

	private void lookInWhatState() {
		System.out.printf("carorient: %f \n", info.getOrientation());

		System.out.printf("targetOrientation: %f \n", targetOrient);

		if (neededRotationGreaterThanBrakeAngle) {
			System.out.println("neededRotationGreaterThanBrakeAngle");
		}
		if (toleranceBool) {
			System.out.println("tolerance");
		}
		if (neededRotationTinierThanBrakeAngle) {
			System.out.println("neddedTotationTinierThanBrakeAngle");
		}
	}

}

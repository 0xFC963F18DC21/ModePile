package zeroxfc.nullpo.custom.modes;

import java.util.Random;

import mu.nu.nullpo.game.component.BGMStatus;
import mu.nu.nullpo.game.component.Block;
import mu.nu.nullpo.game.component.Controller;
import mu.nu.nullpo.game.component.Field;
import mu.nu.nullpo.game.event.EventReceiver;
import mu.nu.nullpo.game.play.GameEngine;
import mu.nu.nullpo.game.play.GameManager;
import mu.nu.nullpo.util.CustomProperties;
import org.apache.log4j.Logger;
import zeroxfc.nullpo.custom.libs.*;

public class Pong extends PuzzleGameEngine {
	private static Logger log = Logger.getLogger(Pong.class);

	private static final int LOCALSTATE_IDLE = 0,
	                         LOCALSTATE_SPAWNING = 1,
	                         LOCALSTATE_INGAME = 2;

	private static final int COLLISION_NONE = 0,
	                         COLLISION_FIELD_TOP = 1,
	                         COLLISION_FIELD_BOTTOM = 2,
	                         COLLISION_PADDLE_PLAYER = 3,
	                         COLLISION_PADDLE_COMPUTER = 4;

	private static final int FIELD_WIDTH = 20,
	                         FIELD_HEIGHT = 12;

	private static final int DIFFICULTY_EASY = 0,
	                         DIFFICULTY_MEDIUM = 1,
	                         DIFFICULTY_HARD = 2;

	private static final String[] DIFFICULTY_NAMES = {
			"EASY",
			"MEDIUM",
			"HARD"
	};

	private static final int DIFFICULTY_COUNT = 3;

	private static final double PLAYER_PADDLE_VELOCITY = 8.0;

	private static final double[] COMPUTER_PADDLE_VELOCITY = {
			4.0, 6.0, 8.0
	};

	private static final double[] COMPUTER_POWER_HIT_CHANCE = {
			0.05, 0.1, 0.15
	};

	private static final double INITIAL_SPEED = 4.0;
	private static final double MAXIMUM_SPEED = 12.0;
	private static final double SPEED_MULTIPLIER = 1.1;

	private static final double UP = Math.PI / 2;
	private static final double DOWN = UP * 3;

	private GameManager owner;
	private EventReceiver receiver;
	private PhysicsObject paddlePlayer, paddleComputer, ball;
	private Random initialDirectionRandomiser, computerActionRandomiser;
	private int fieldBoxMinX, fieldBoxMinY, fieldBoxMaxX, fieldBoxMaxY;  // FIELD COLLISION BOUNDS.
	private int bg, bgm, difficulty;
	private int recentCollision;
	private int playerScore, computerScore;

	@Override
	public String getName() {
		return "PONG";
	}

	@Override
	public void playerInit(GameEngine engine, int playerID) {
		owner = engine.owner;
		receiver = engine.owner.receiver;

		bg = 0;
		bgm = -1;
		difficulty = 0;
		localState = LOCALSTATE_IDLE;
		recentCollision = 0;
		playerScore = 0;
		computerScore = 0;

		loadSetting(owner.modeConfig);
		owner.backgroundStatus.bg = bg;
		owner.bgmStatus.bgm = -1;
		engine.framecolor = GameEngine.FRAME_COLOR_PINK;
	}

	@Override
	public boolean onSetting(GameEngine engine, int playerID) {
		owner.backgroundStatus.bg = bg;
		owner.bgmStatus.bgm = -1;

		// Menu
		if(!engine.owner.replayMode) {
			// Configuration changes
			int change = updateCursor(engine, 1, playerID);

			if(change != 0) {
				engine.playSE("change");

				switch(engine.statc[2]) {
					case 0:
						bg += change;
						if (bg > 19) bg = 0;
						if (bg < 0) bg = 19;
						break;
					case 1:
						bgm += change;
						if (bgm > 15) bgm = -1;
						if (bgm < -1) bgm = 15;
						break;
				}
			}

			// Confirm
			if(engine.ctrl.isPush(Controller.BUTTON_A) && (engine.statc[3] >= 5)) {
				engine.playSE("decide");
				saveSetting(owner.modeConfig);
				receiver.saveModeConfig(owner.modeConfig);
				return false;
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) {
				engine.quitflag = true;
			}

			engine.statc[3]++;
		}
		// Replay
		else {
			engine.statc[3]++;
			engine.statc[2] = -1;

			if (engine.statc[3] >= 60) {
				return false;
			}
		}

		return true;
	}

	@Override
	public void renderSetting(GameEngine engine, int playerID) {
		drawMenu(engine, playerID, receiver, 0, EventReceiver.COLOR_BLUE, 0,
				"BG", (bg >= 0) ? String.valueOf(bg) : "AUTO",
				"BGM", (bgm >= 0) ? String.valueOf(bgm) : "DISABLED");
	}

	@Override
	public boolean onReady(GameEngine engine, int playerID) {
		// 横溜め
		if(engine.ruleopt.dasInReady && engine.gameActive) engine.padRepeat();
		else if(engine.ruleopt.dasRedirectInDelay) { engine.dasRedirect(); }

		// Initialization
		if(engine.statc[0] == 0) {
			// fieldInitialization
			engine.ruleopt.fieldWidth = FIELD_WIDTH;
			engine.ruleopt.fieldHeight = FIELD_HEIGHT;
			engine.ruleopt.fieldHiddenHeight = 0;
			engine.displaysize = 0;

			engine.ruleopt.nextDisplay = 0;
			engine.ruleopt.holdEnable = false;

			engine.fieldWidth = engine.ruleopt.fieldWidth;
			engine.fieldHeight = engine.ruleopt.fieldHeight;
			engine.fieldHiddenHeight = engine.ruleopt.fieldHiddenHeight;
			engine.field = new Field(engine.fieldWidth, engine.fieldHeight, engine.fieldHiddenHeight, true);

			fieldBoxMinX = receiver.getFieldDisplayPositionX(engine, playerID) + 4;
			fieldBoxMinY = receiver.getFieldDisplayPositionY(engine, playerID) + 52;
			fieldBoxMaxX = fieldBoxMinX + (FIELD_WIDTH * 16) - 1;
			fieldBoxMaxY = fieldBoxMinY + (FIELD_HEIGHT * 16) - 1;

			initialDirectionRandomiser = new Random(engine.randSeed);
			computerActionRandomiser = new Random(engine.randSeed + 1);

			resetPhysicsObjects(true);
			playerScore = 0;
			computerScore = 0;


			if(!engine.readyDone) {
				//  button input状態リセット
				engine.ctrl.reset();
				// ゲーム中 flagON
				engine.gameActive = true;
				engine.gameStarted = true;
				engine.isInGame = true;
			}

			rankingRank = -1;
		}

		// READY音
		if(engine.statc[0] == engine.readyStart) engine.playSE("ready");

		// GO音
		if(engine.statc[0] == engine.goStart) engine.playSE("go");

		// 開始
		if(engine.statc[0] >= engine.goEnd) {
			if(!engine.readyDone) engine.owner.bgmStatus.bgm = bgm;
			if(engine.owner.mode != null) engine.owner.mode.startGame(engine, playerID);
			engine.owner.receiver.startGame(engine, playerID);
			engine.stat = GameEngine.STAT_CUSTOM;
			localState = LOCALSTATE_SPAWNING;
			engine.timerActive = true;
			engine.resetStatc();
			if(!engine.readyDone) {
				engine.startTime = System.nanoTime();
				//startTime = System.nanoTime()/1000000L;
			}
			engine.readyDone = true;
			return true;
		}

		engine.statc[0]++;

		return true;
	}

	private void resetPhysicsObjects(boolean resetPaddles) {
		DoubleVector position = new DoubleVector((fieldBoxMinX + fieldBoxMaxX) / 2,(fieldBoxMinY + fieldBoxMaxY) / 2, false);
		ball = new PhysicsObject(position, DoubleVector.zero(), -1, 1, 1, PhysicsObject.ANCHOR_POINT_MM, Block.BLOCK_COLOR_GREEN);
		ball.PROPERTY_Static = false;
		recentCollision = 0;

		if (resetPaddles) {
			DoubleVector playerPosition = new DoubleVector(fieldBoxMinX,(fieldBoxMinY + fieldBoxMaxY) / 2, false);
			DoubleVector computerPosition = new DoubleVector(fieldBoxMaxX,(fieldBoxMinY + fieldBoxMaxY) / 2, false);

			paddlePlayer = new PhysicsObject(playerPosition, DoubleVector.zero(), -1, 1, 4, PhysicsObject.ANCHOR_POINT_ML, Block.BLOCK_COLOR_CYAN);
			paddleComputer = new PhysicsObject(computerPosition, DoubleVector.zero(), -1, 1, 4, PhysicsObject.ANCHOR_POINT_MR, Block.BLOCK_COLOR_RED);

			paddlePlayer.PROPERTY_Static = false;
			paddleComputer.PROPERTY_Static = false;
		}
	}

	private void setBallRandomDirection() {
		double coeff = initialDirectionRandomiser.nextDouble();
		final double pot = Math.PI / 2;
		double angle;

		if (coeff < 0.5) {
			final double tpof = Math.PI * 0.75;
			angle = tpof + (pot * initialDirectionRandomiser.nextDouble());
		} else {
			final double optpof = (Math.PI * 0.75) + Math.PI;
			angle = optpof + (pot * initialDirectionRandomiser.nextDouble());
			if (angle >= Math.PI * 2) angle -= (Math.PI * 2);
		}

		ball.velocity.setDirection(angle);
		ball.velocity.setMagnitude(INITIAL_SPEED);
	}

	@Override
	public boolean onGameOver(GameEngine engine, int playerID) {
		if(engine.lives <= 0) {
			// もう復活できないとき
			if(engine.statc[0] == 0) {
				engine.gameEnded();
				engine.blockShowOutlineOnly = false;
				if(owner.getPlayers() < 2) owner.bgmStatus.bgm = BGMStatus.BGM_NOTHING;

				if(engine.field.isEmpty()) {
					engine.statc[0] = engine.field.getHeight() + 1;
				} else {
					engine.resetFieldVisible();
				}
			}

			if(engine.statc[0] < engine.field.getHeight() + 1) {
				for(int i = 0; i < engine.field.getWidth(); i++) {
					if(engine.field.getBlockColor(i, engine.field.getHeight() - engine.statc[0]) != Block.BLOCK_COLOR_NONE) {
						Block blk = engine.field.getBlock(i, engine.field.getHeight() - engine.statc[0]);

						if(blk != null) {
							if (blk.color > Block.BLOCK_COLOR_NONE) {
								if(!blk.getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE)) {
									blk.color = Block.BLOCK_COLOR_GRAY;
									blk.setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);
								}
								blk.darkness = 0.3f;
								blk.elapsedFrames = -1;
							}
						}
					}
				}
				engine.statc[0]++;
			} else if(engine.statc[0] == engine.field.getHeight() + 1) {
				engine.playSE("gameover");
				engine.statc[0]++;
			} else if(engine.statc[0] < engine.field.getHeight() + 1 + 180) {
				if((engine.statc[0] >= engine.field.getHeight() + 1 + 60) && (engine.ctrl.isPush(Controller.BUTTON_A))) {
					engine.statc[0] = engine.field.getHeight() + 1 + 180;
				}

				engine.statc[0]++;
			} else {
				if(!owner.replayMode || owner.replayRerecord) owner.saveReplay();

				for(int i = 0; i < owner.getPlayers(); i++) {
					if((i == playerID) || (engine.gameoverAll)) {
						if(owner.engine[i].field != null) {
							owner.engine[i].field.reset();
						}
						owner.engine[i].resetStatc();
						owner.engine[i].stat = GameEngine.STAT_RESULT;
					}
				}
			}
		} else {
			// 復活できるとき
			if(engine.statc[0] == 0) {
				engine.blockShowOutlineOnly = false;
				engine.playSE("died");

				engine.resetFieldVisible();

				for(int i = (engine.field.getHiddenHeight() * -1); i < engine.field.getHeight(); i++) {
					for(int j = 0; j < engine.field.getWidth(); j++) {
						if(engine.field.getBlockColor(j, i) != Block.BLOCK_COLOR_NONE) {
							engine.field.setBlockColor(j, i, Block.BLOCK_COLOR_GRAY);
						}
					}
				}

				engine.statc[0] = 1;
			}

			if(!engine.field.isEmpty()) {
				engine.field.pushDown();
			} else if(engine.statc[1] < engine.getARE()) {
				engine.statc[1]++;
			} else {
				engine.lives--;
				engine.resetStatc();
				engine.stat = GameEngine.STAT_CUSTOM;
			}
		}
		return true;
	}

	/*
	 * Called when saving replay
	 */
	@Override
	public void saveReplay(GameEngine engine, int playerID, CustomProperties prop) {
		saveSetting(prop);

		if((!owner.replayMode)) {
			receiver.saveModeConfig(owner.modeConfig);
		}
	}

	@Override
	public boolean onCustom(GameEngine engine, int playerID) {
		boolean updateTimer = false;
		// Override this.

		switch (localState) {
			case LOCALSTATE_SPAWNING:
				updateTimer = statSpawning(engine, playerID);
				break;
			case LOCALSTATE_INGAME:
				updateTimer = statIngame(engine, playerID);
				break;
			default:
				break;
		}

		if (updateTimer) engine.statc[0]++;
		return true;
	}

	private boolean statSpawning(GameEngine engine, int playerID) {
		if (engine.statc[0] == 0) {
			resetPhysicsObjects(false);
		} else if (engine.statc[0] == 60) {
			localState = LOCALSTATE_INGAME;
			setBallRandomDirection();
			engine.resetStatc();
			engine.playSE("levelup");
			return false;
		}

		return true;
	}

	private boolean statIngame(GameEngine engine, int playerID) {
		// TODO: Write player and computer input code here.

		/*
		PhysicsObject[] testClones = new PhysicsObject[6];  // 6-step collision checker.
		for (int i = 0; i < testClones.length; i++) {
			testClones[i] = ball.clone();
			testClones[i].velocity.setMagnitude((testClones[i].velocity.getMagnitude() / 6) * (i + 1));
			testClones[i].move();
		}

		int index = 0;
		for (int i = 0; i < testClones.length; i++) {
			index = i;
			if (PhysicsObject.checkCollision(testClones[i], paddlePlayer)) {
				index = i - 1;
				recentCollision = COLLISION_PADDLE_PLAYER;
				break;
			} else if (PhysicsObject.checkCollision(testClones[i], paddleComputer)) {
				index = i - 1;
				recentCollision = COLLISION_PADDLE_COMPUTER;
				break;
			} else if (testClones[i].getMinY() < fieldBoxMinY) {
				index = i - 1;
				recentCollision = COLLISION_FIELD_TOP;
				break;
			} else if (testClones[i].getMaxY() > fieldBoxMaxY) {
				index = i - 1;
				recentCollision = COLLISION_FIELD_BOTTOM;
				break;
			}

			recentCollision = COLLISION_NONE;
		}

		if (index != -1) {
			ball = testClones[index].clone();
		} else {
			// Collision imminent. Must reflect;
			ball = testClones[0].clone();

			final double pof = Math.PI / 4;
			final double tpof = Math.PI * 0.75;
			final double optpof = (Math.PI * 0.75) + Math.PI;
			final double maxOffset = 32.0;
			double yCentreOffset;
			double angle;

			switch (recentCollision) {
				case COLLISION_FIELD_TOP:
				case COLLISION_FIELD_BOTTOM:
					PhysicsObject.reflectVelocity(ball.velocity, true);
					break;
				case COLLISION_PADDLE_PLAYER:
					yCentreOffset = ball.position.getY() - paddlePlayer.position.getY();
					angle = pof * (yCentreOffset / maxOffset);
					ball.velocity.setDirection(angle);

					break;
				case COLLISION_PADDLE_COMPUTER:
					yCentreOffset = ball.position.getY() - paddleComputer.position.getY();
					angle = (-1 * (pof * (yCentreOffset / maxOffset))) + Math.PI;
					ball.velocity.setDirection(angle);

					break;
				default:
					break;
			}
		}
		*/
		ball.move();
		if (PhysicsObject.checkCollision(ball, paddlePlayer)) {
			recentCollision = COLLISION_PADDLE_PLAYER;
			log.debug("COLLISION_PLAYER");
		} else if (PhysicsObject.checkCollision(ball, paddleComputer)) {
			recentCollision = COLLISION_PADDLE_COMPUTER;
			log.debug("COLLISION_COM");
		} else if (ball.getMinY() < fieldBoxMinY) {
			recentCollision = COLLISION_FIELD_TOP;
			log.debug("COLLISION_FLD_TOP");
		} else if (ball.getMaxY() > fieldBoxMaxY) {
			recentCollision = COLLISION_FIELD_BOTTOM;
			log.debug("COLLISION_FLD_BOTTOM");
		} else {
			recentCollision = COLLISION_NONE;
		}

		final double pof = Math.PI / 4;
		final double maxOffset = 32.0;
		double yCentreOffset;
		double angle;
		switch (recentCollision) {
			case COLLISION_FIELD_TOP:
			case COLLISION_FIELD_BOTTOM:
				PhysicsObject.reflectVelocity(ball.velocity, true);
				log.debug("WALL REFLECT");
				break;
			case COLLISION_PADDLE_PLAYER:
				yCentreOffset = ball.position.getY() - paddlePlayer.position.getY();
				angle = pof * (yCentreOffset / maxOffset);
				ball.velocity.setDirection(angle);
				log.debug("PLAYER REFLECT");

				break;
			case COLLISION_PADDLE_COMPUTER:
				yCentreOffset = ball.position.getY() - paddleComputer.position.getY();
				angle = (-1 * (pof * (yCentreOffset / maxOffset))) + Math.PI;
				ball.velocity.setDirection(angle);
				log.debug("COM REFLECT");

				break;
			default:
				break;
		}

		if (ball.getMinX() <= fieldBoxMinX) {
			engine.resetStatc();
			localState = LOCALSTATE_SPAWNING;
			computerScore++;
		} else if (ball.getMaxX() >= fieldBoxMaxX) {
			engine.resetStatc();
			localState = LOCALSTATE_SPAWNING;
			playerScore++;
		}

		return false;
	}

	/*
	 * Render score
	 */
	@Override
	public void renderLast(GameEngine engine, int playerID) {
		if(owner.menuOnly) return;

		if( engine.stat == GameEngine.STAT_SETTING ) {
			receiver.drawScoreFont(engine, playerID, 0, 0, getName(), EventReceiver.COLOR_BLUE);
			receiver.drawScoreFont(engine, playerID, 0, 1, "(" + DIFFICULTY_NAMES[difficulty] + " MODE)", EventReceiver.COLOR_BLUE);

			switch (difficulty) {
				case DIFFICULTY_EASY:
					receiver.drawScoreFont(engine, playerID, 0, 3, "SLOW COMPUTER PADDLE");
					receiver.drawScoreFont(engine, playerID, 0, 4, "5% SPEED-HIT CHANCE");
					break;
				case DIFFICULTY_MEDIUM:
					receiver.drawScoreFont(engine, playerID, 0, 3, "MEDIUM COMPUTER PADDLE");
					receiver.drawScoreFont(engine, playerID, 0, 4, "10% SPEED-HIT CHANCE");
					break;
				case DIFFICULTY_HARD:
					receiver.drawScoreFont(engine, playerID, 0, 3, "FAST COMPUTER PADDLE");
					receiver.drawScoreFont(engine, playerID, 0, 4, "15% SPEED-HIT CHANCE");
					break;
				default:
					break;
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 8, 0, getName(), EventReceiver.COLOR_BLUE);
			receiver.drawScoreFont(engine, playerID, 8, 1, "(" + DIFFICULTY_NAMES[difficulty] + " MODE)", EventReceiver.COLOR_BLUE);

			receiver.drawScoreFont(engine, playerID, 8, 3, "1P PTS.", EventReceiver.COLOR_CYAN);
			receiver.drawScoreFont(engine, playerID, 8, 4, String.valueOf(playerScore));

			receiver.drawScoreFont(engine, playerID, 8, 6, "COM. PTS.", EventReceiver.COLOR_RED);
			receiver.drawScoreFont(engine, playerID, 8, 7, String.valueOf(computerScore));

			if (paddleComputer != null) paddleComputer.draw(receiver, engine, playerID);
			if (paddlePlayer != null) paddlePlayer.draw(receiver, engine, playerID);
			if (ball != null) ball.draw(receiver, engine, playerID);
		}
	}

	/*
	 * Render results screen
	 */
	@Override
	public void renderResult(GameEngine engine, int playerID) {
		receiver.drawMenuFont(engine, playerID, 0, 0, "1P PTS.", EventReceiver.COLOR_CYAN);
		receiver.drawMenuFont(engine, playerID, 0, 1, String.format("%10s", playerScore));

		receiver.drawMenuFont(engine, playerID, 0, 2, "COM. PTS.", EventReceiver.COLOR_RED);
		receiver.drawMenuFont(engine, playerID, 0, 3, String.format("%10s", computerScore));
	}


	/**
	 * Load settings from property file
	 * @param prop Property file
	 */
	private void loadSetting(CustomProperties prop) {
		bg = prop.getProperty("pong.bg", 0);
		bgm = prop.getProperty("pong.bgm", -1);
		difficulty = prop.getProperty("pong.difficulty", 0);
	}

	/**
	 * Save settings to property file
	 * @param prop Property file
	 */
	private void saveSetting(CustomProperties prop) {
		prop.setProperty("pong.bg", bg);
		prop.setProperty("pong.bgm", bgm);
		prop.setProperty("pong.difficulty", difficulty);
	}
}

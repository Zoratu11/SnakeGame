package com.packtpub.snake;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.Random;

public class GameActivity extends Activity {

    Canvas canvas;
    SnakeView snakeView;

    Bitmap headBitmap;
    Bitmap bodyBitmap;
    Bitmap tailBitmap;
    Bitmap appleBitmap;

    //Sound method used whenever I need sound
    public void playSound(int soundResid) {
        final MediaPlayer mp = MediaPlayer.create(this, soundResid);
        mp.start();
    }

    //For snake movement
    int directionOfTravel = 0;
    //0 = up, 1 = right, 2 = down, 3 = left

    int screenWidth;
    int screenHeight;
    int topGap;

    //Stats
    long lastFrameTime;
    int fps;
    int score;
    int hi;

    //game objects
    int[] snakeX;
    int[] snakeY;
    int snakeLength;
    int appleXOne;
    int appleYOne;
    int appleXTwo;
    int appleYTwo;
    int appleXThree;
    int appleYThree;
    int appleXFour;
    int appleYFour;

    //The size in pixels of a place on the game board
    int blockSize;
    int numBlocksWide;
    int numBlocksHigh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        configureDisplay();
        snakeView = new SnakeView(this);
        setContentView(snakeView);
    }

    class SnakeView extends SurfaceView implements Runnable {
        Thread ourThread = null;
        SurfaceHolder ourHolder;
        volatile boolean playingSnake;
        Paint paint;

        public SnakeView(Context context) {
            super(context);
            ourHolder = getHolder();
            paint = new Paint();

            //Even my 10 year old playtester couldn't
            //get a snake this long
            snakeX = new int[200];
            snakeY = new int[200];

            //Our starting snake
            getSnake();
            //Get an apple for the snake
            getApple();
        }

        public void getSnake() {
            snakeLength = 3;
            //Start the snake in the middle of the screen
            snakeX[0] = numBlocksWide/2;//20
            snakeY[0] = numBlocksHigh/2;//10

            //Body
            snakeX[1] = snakeX[0] - 1;//19
            snakeY[1] = snakeY[0];

            //Tail
            snakeX[2] = snakeX[1] - 1;//18
            snakeY[2] = snakeY[0];
        }

        public void getApple() {
            Random random = new Random();
            appleXOne = random.nextInt(numBlocksWide - 1) + 1;
            appleYOne = random.nextInt(numBlocksHigh - 1) + 1;
            appleXTwo = appleXOne + 1;
            appleYTwo = appleYOne;
            appleXThree = appleXOne + 1;
            appleYThree = appleYOne + 1;
            appleXFour = appleXOne;
            appleYFour = appleXOne + 1;//There are 4 points on the apple so its easier to hit
            //appleX = random.nextInt(numBlocksWide - 1) + 1;
            //appleY = random.nextInt(numBlocksHigh - 1) + 1;
        }

        @Override
        public void run() {
            while (playingSnake) {
                updateGame();
                drawGame();
                controlFPS();
            }
        }

        public void updateGame() {
            //checks if the snake got the apple
            if ((snakeX[0] == appleXOne && snakeY[0] == appleYOne) ||
                    (snakeX[0] == appleXTwo && snakeY[0] == appleYTwo) ||
                    (snakeX[0] == appleXThree && snakeY[0] == appleYThree) ||
                    (snakeX[0] == appleXFour && snakeY[0] == appleYFour)) {
                //Grow the snake
                snakeLength++;
                //Get a new apple
                getApple();
                //add to the score
                score = score + snakeLength;
                playSound(R.raw.sample2);
            }
            //Move the body - starting at the back
            for (int i = snakeLength; i > 0; i--) {
                snakeX[i] = snakeX[i - 1];
                snakeY[i] = snakeY[i - 1];
            }

            //Move the head in the appropriate direction
            switch (directionOfTravel) {
                case 0://up
                    snakeY[0]--;
                    break;
                case 1:
                    snakeX[0]++;
                    break;
                case 2:
                    snakeY[0]++;
                    break;
                case 3:
                    snakeX[0]--;
                    break;
            }

            //Have we had an accident?
            boolean dead = false;
            //with a wall
            if (snakeX[0] == -1) dead = true;
            if (snakeY[0] == -1) dead = true;
            if (snakeX[0] == numBlocksWide) dead = true;
            if (snakeY[0] == numBlocksHigh) dead = true;
            //Or have we ate ourself?
            for (int i = snakeLength - 1; i > 0; i--) {
                if (i > 4 && snakeX[0] == snakeX[i] && snakeY[0] == snakeY[i]) {
                    dead = true;
                }
            }
            if (dead) {
                //Start over
                playSound(R.raw.sample1);
                score = 0;
                getSnake();
            }
        }

        public void drawGame() {
            if (ourHolder.getSurface().isValid()) {
                canvas = ourHolder.lockCanvas();
                //Paint paint = new paint();
                canvas.drawColor(Color.GREEN);
                paint.setColor(Color.argb(255, 255, 255, 255));
                paint.setTextSize(topGap / 2);
                canvas.drawText("Score:" + score + " Hi:" + hi,
                        10, topGap - 6, paint);
                //Draw a border - 4 lines, top right, bottom, left
                paint.setStrokeWidth(3);//3 pixel border
                canvas.drawLine(1, topGap, screenWidth - 1, topGap, paint);
                canvas.drawLine(1, screenHeight - 1, screenWidth - 1, screenHeight - 1, paint);
                canvas.drawLine(1, topGap, 1, screenHeight - 1, paint);
                canvas.drawLine(screenWidth - 1, topGap, screenWidth - 1, screenHeight - 1, paint);

                //Draw the snake
                canvas.drawBitmap(headBitmap, snakeX[0]*blockSize,//420
                        (snakeY[0]*blockSize)+topGap, paint);
                //Draw the body
                for (int i = 1; i < snakeLength - 1; i++) {
                    canvas.drawBitmap(bodyBitmap, snakeX[i]*blockSize,
                            (snakeY[i]*blockSize)+topGap, paint);
                }
                //Draw the tail
                canvas.drawBitmap(tailBitmap, snakeX[snakeLength - 1]*blockSize,
                        (snakeY[snakeLength - 1]*blockSize)+topGap, paint);
                //Draw the apple
                canvas.drawBitmap(appleBitmap, appleXOne*blockSize,
                        (appleYOne*blockSize) + topGap, paint);

                ourHolder.unlockCanvasAndPost(canvas);
            }
        }

        public void controlFPS() {
            long timeThisFrame =
                    (System.currentTimeMillis() - lastFrameTime);
            long timeToSleep = 100 - timeThisFrame;
            if (timeThisFrame > 0) {
                fps = (int) (1000 / timeThisFrame);
            }
            if (timeToSleep > 0) {

                try {
                    ourThread.sleep(timeToSleep);
                } catch (InterruptedException e) {
                }
            }

            lastFrameTime = System.currentTimeMillis();
        }

        public void pause() {
            playingSnake = false;
            try {
                ourThread.join();
            } catch (InterruptedException e) {
            }
        }

        public void resume() {
            playingSnake = true;
            ourThread = new Thread(this);
            ourThread.start();
        }

        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {
            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_UP:
                    if (motionEvent.getX() >= screenWidth / 2) {
                        //Turn right
                        directionOfTravel++;
                        //no such direction

                        if (directionOfTravel == 4) {
                            //loop back to 0(up)
                            directionOfTravel = 0;
                        }
                    } else {
                        //Turn left
                        directionOfTravel--;
                        if (directionOfTravel == -1) {//No such direction
                            //loop back to 3(left)
                            directionOfTravel = 3;
                        }

                    }
            }
            return true;
        }
    }
        @Override
        protected void onStop() {
            super.onStop();

            while (true) {
                snakeView.pause();
                break;
            }
            finish();
        }

        @Override
        protected void onResume() {
            super.onResume();
            snakeView.resume();
        }

        @Override
        protected void onPause() {
            super.onPause();
            snakeView.pause();
        }

        public boolean onKeyDown(int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {

                snakeView.pause();


                Intent i = new Intent(this,
                        MainActivity.class);
                startActivity(i);
                finish();
                return true;
            }
            return false;
        }

        public void configureDisplay() {
            //Find out the height of the screen
            Display display =
                    getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            screenWidth = size.x;
            screenHeight = size.y;
            topGap = screenHeight / 14;//34
            //Determine the size of each block
            blockSize = screenWidth / 40;//21

            //Determine how many blocks will fit into the height and width
            //Leave on block for the score at the top
            numBlocksWide = 40;
            numBlocksHigh = ((screenHeight - topGap)) / blockSize;//21

            //Load and scale bitmaps
            headBitmap =
                    BitmapFactory.decodeResource(getResources(),
                            R.drawable.head);
            bodyBitmap =
                    BitmapFactory.decodeResource(getResources(),
                            R.drawable.body);
            tailBitmap =
                    BitmapFactory.decodeResource(getResources(),
                            R.drawable.tail);
            appleBitmap =
                    BitmapFactory.decodeResource(getResources(),
                            R.drawable.apple);

            //Scale the bitmaps to match blockSize
            headBitmap = Bitmap.createScaledBitmap(headBitmap, blockSize,
                    blockSize, false);
            bodyBitmap = Bitmap.createScaledBitmap(bodyBitmap, blockSize,
                    blockSize, false);
            tailBitmap = Bitmap.createScaledBitmap(tailBitmap, blockSize,
                    blockSize, false);
            appleBitmap = Bitmap.createScaledBitmap(appleBitmap, blockSize*2,
                    blockSize*2, false);
        }
}



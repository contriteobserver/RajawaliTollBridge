package org.rajawali3d.tinyloaderbridge;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

import org.rajawali3d.Object3D;
import org.rajawali3d.lights.ALight;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.renderer.Renderer;
import org.rajawali3d.view.SurfaceView;

public class MainActivity extends AppCompatActivity {
    SurfaceView view;
    ObjRenderer renderer;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        renderer = new ObjRenderer(getApplicationContext());
        view = findViewById(R.id.glsl_content);
        view.setSurfaceRenderer(renderer);
    }

    @Override
    protected void onPause() {
        super.onPause();
        renderer.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderer.onResume();
    }

    class ObjRenderer extends Renderer {

        public ObjRenderer(Context context) {
            super(context);
        }

        @Override
        protected void initScene() {
            try {
                getCurrentScene().setBackgroundColor(Color.CYAN & Color.DKGRAY);

                ALight key = new DirectionalLight(-8,-8,-8);
                key.setPower(1);
                getCurrentScene().addLight(key);

                Bridge bridge = new Bridge(getAssets(), "cube.obj", "cube.mtl");
                bridge.parse();

                Object3D obj = bridge.getParsedObj();
                getCurrentScene().addChild(obj);

                getCurrentCamera().setPosition(-5,3,-4);
                getCurrentCamera().setLookAt(obj.getPosition());
            } catch (Exception e) {
                Log.e(getLocalClassName() + ".initDefaultScene", e.getMessage());
            }
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

        }

        @Override
        public void onTouchEvent(MotionEvent event) {
        }
    }
}
package com.example.rxjavawithcallback;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ProgressBar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private final static int INVALID = -1;

    private interface ITaskCallback {
        void getUpdate(Integer progress);
    }

    private class ProgressEmitter implements ITaskCallback {
        final private ObservableEmitter<Integer> emitter;

        private ProgressEmitter(ObservableEmitter<Integer> emitter) {
            this.emitter = emitter;
        }

        @Override
        public void getUpdate(Integer progress) {
            if (progress.intValue() >= 0) {
                emitter.onNext(progress);
            } else if(progress.intValue() == 100) {
                emitter.onNext(progress);
                emitter.onComplete();
            } else {
                emitter.onError(new Exception(progress.toString()));
            }
        }
    }

    private class ProgressObserver extends DisposableObserver<Integer> {

        private ProgressBar pb;

        private ProgressObserver(ProgressBar pb) {
            this.pb = pb;
        }

        @Override
        public void onNext(@NonNull Integer integer) {
            pb.setProgress(integer.intValue());
        }

        @Override
        public void onError(@NonNull Throwable e) {

        }

        @Override
        public void onComplete() {

        }
    }

    private int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    private class ObservableTask {

        public Observable<Integer> observableUpdate() {
            return Observable.create(new ObservableOnSubscribe<Integer>() {
                @Override
                public void subscribe(@NonNull ObservableEmitter<Integer> emitter) throws Throwable {
                    update(new ProgressEmitter(emitter));
                }
            });
        }

        public void update(ITaskCallback callback) {
            for (int i = 0; i <= 100; i += 10) {
                try {
                    Thread.sleep(getRandomNumber(10, 1000));
                    callback.getUpdate(Integer.valueOf(i));
                } catch (Exception e) {
                    System.out.println(e.getStackTrace().toString());
                    callback.getUpdate(Integer.valueOf(INVALID));
                }
            }
        }
    }

    private ProgressBar pb1;
    private ProgressBar pb2;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pb1 = findViewById(R.id.progressBar1);
        pb2 = findViewById(R.id.progressBar2);

        pb1.setProgress(0);
        pb2.setProgress(0);

        executorService = new ThreadPoolExecutor(4, 5, 60L, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());

        ObservableTask task1 = new ObservableTask();
        ObservableTask task2 = new ObservableTask();

        task1.observableUpdate()
                .subscribeOn(Schedulers.from(executorService))
                .subscribe(new ProgressObserver(pb1));

        task2.observableUpdate()
                .subscribeOn(Schedulers.from(executorService))
                .subscribe(new ProgressObserver(pb2));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        executorService.shutdown();
    }
}
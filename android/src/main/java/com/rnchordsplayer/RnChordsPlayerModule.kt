package com.rnchordsplayer

import kotlin.math.pow
import kotlin.math.roundToLong

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.Callback

import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments
import com.facebook.react.modules.core.DeviceEventManagerModule

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.CountDownTimer

import android.util.Log

class RnChordsPlayerModule(val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  private var soundPool: SoundPool? = null;
  private var timer: CountDownTimer? = null;

  private val DEFAULT_NOTE: String = "pause";
  private var playing: Boolean = true;
  private val soundsIds: MutableMap<String, Int> = mutableMapOf<String, Int>(); // словарь с проигрываемыми звуками
                                            // хранит текстовое название и ID загруженного soundpool звука
  private val soundsNames: MutableList<String> = mutableListOf<String>(); // список проигрываения с названиями нот/боев

  private val EVENT_STRIKE: String = "Strike";
  private val ALLOW_STRIKES = arrayOf("down", "up", "x", "pause")
  


  override fun getName(): String {
    return NAME
  }

  private fun sendEvent(reactContext: ReactContext, eventName: String, params: WritableMap?) {
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, params)
  }

  @ReactMethod
  fun addListener(eventName: String) {
    Log.i("ReactNative", "add event listener called");
  }

  @ReactMethod
  fun removeListeners(count: Int) {
      Log.i("ReactNative", "remove event listener called")
  }

  /** Очищает timer, все загруженные звуки и сам soundPool */
  fun releaseAll(): Unit {
    if (timer != null) {
      timer?.cancel();
      timer = null;
    }
    if (soundPool != null) {
      for((name,soundId) in soundsIds) {
        soundPool?.unload(soundId);
      }
      soundsIds.clear();
      soundsNames.clear();
      soundPool?.release();
      soundPool = null;
    }
  }

  /** Инициализация soundPool */
  fun initializeSoundPool(): Unit {
    val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
    soundPool = SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(attributes)
                .build();
  }

  /** 
  * Инициализирует soundPool и загружает список звуков для проигрывания
  * @param notes список строк с названиями проигрываемых звуков, полученный из js
  */
  fun preparationPlaying(notes: ReadableArray?): Unit {
    releaseAll();
    initializeSoundPool();
    
    val notesAmount: Int = notes?.size() ?: 0;

    for (noteIndex in 0..notesAmount-1) {
      val note: String = notes?.getString(noteIndex) ?: DEFAULT_NOTE;
      if (!ALLOW_STRIKES.contains(note))
          continue;
      soundsNames.add(note);
      if (note!="pause" && !soundsIds.contains(note) ) {
        soundsIds.put(note, loadSound(note));
      }      
    }
  }

  /**
  * @param duration время звучания ноты. 0 - "целая", 1 - "половинная" и т.д
  * @param bpm bpm
  * @return время звучания ноты в миллисекундах
  */
  fun getNoteDurationInMs(duration: Int, bpm: Int): Long {
    return (60.0 * 1000.0 * 4.0 / bpm / 2.0.pow(duration)).roundToLong();
  }

  /**
  * Загружает звук и возвращает его идентификатор. Если такого звука нет, загружает "паузу"
  * @param soundName: String название ноты/удара.
  * @return идентификатор загруженного звука
  */
  fun loadSound(soundName: String): Int {
    if (soundPool == null) return 0;

    return when (soundName) {
        "down" -> soundPool?.load(reactContext, R.raw.down, 1) ?: 0;
        "up" -> soundPool?.load(reactContext, R.raw.up, 1) ?: 0;
        "x" -> soundPool?.load(reactContext, R.raw.x, 1) ?: 0;
        "pause" -> 0;
        else -> {
          0;
        }
    }
  }

  /**
  * Проигрывает звуки, которые загружены в список soundsNames
  * @param allDuration полное время звучания боя/перебора
  * @param interval расстояние между двумя ударами/нотами 
  */
  fun playSounds(allDuration: Long, interval: Long): Unit {
    var currentNoteIndex: Int = 0; // номер текущего удара
    timer = object: CountDownTimer(allDuration, interval) {
      override fun onTick(milliseconds: Long) {
        val currentStrike: String = soundsNames.elementAt(currentNoteIndex);
        if (currentStrike != "pause") {  
          soundPool?.play(soundsIds.getOrDefault(currentStrike, 0), 1F, 1F, 1, 0, 1F);    
        }

        val eventParams = Arguments.createMap().apply {
          putInt("strikeIndex", currentNoteIndex)
        }
        sendEvent(reactContext, EVENT_STRIKE, eventParams)

        currentNoteIndex++;
      }
      override fun onFinish() {
        if (playing) {
          currentNoteIndex = 0;
          this.start();
        } else {
          releaseAll();
        }
      }
    }

    var amounOfSoundsLoaded = 0;
    soundPool?.setOnLoadCompleteListener(SoundPool.OnLoadCompleteListener{soundPool, sampleId, status -> 
      amounOfSoundsLoaded++;
      if (amounOfSoundsLoaded == soundsIds.size) {
        playing = true;
        timer?.start();
      }
    });
  }

  /** Останавливает проигрывание и освобождает ресурсы */
  @ReactMethod fun stop() {
    playing = false;
    releaseAll();
  }

  /**
  * ReactMethod, который позволяет проигрывать гитарный бой
  * @param beat Словарь со списком боев и длительностью ноты
  * @param bpm Количество ударов метронома в минуту (количество нот длительностью 1/4 в минуте)
  */
  @ReactMethod fun playBeat(beat: ReadableMap, bpm: Int) {
    val strikes: ReadableArray? = beat.getArray("strikes");
    val duration: Int = beat.getInt("duration") ?: 2; // Значение по умолчанию - 1/4 длина ноты
    val strikesAmount: Int = strikes?.size() ?: 0;  // количество ударов в бое
    val baseIntervalMs: Long = getNoteDurationInMs(duration, bpm); // время звучания одной ноты/боя
    val allBeatDuration: Long = (baseIntervalMs * strikesAmount).toLong();
    preparationPlaying(strikes);
    playSounds(allBeatDuration, baseIntervalMs);
  }

  companion object {
    const val NAME = "RnChordsPlayer"
  }
}

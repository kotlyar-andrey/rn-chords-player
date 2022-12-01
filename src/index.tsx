import { NativeEventEmitter, NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'rn-chords-player' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const RnChordsPlayer = NativeModules.RnChordsPlayer
  ? NativeModules.RnChordsPlayer
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export interface Beat {
  strikes: string[];
  duration: number;
}

export function multiply(a: number, b: number): Promise<number> {
  return RnChordsPlayer.multiply(a, b);
}

export function playBeat(beat: Beat, bpm: number): void {
  RnChordsPlayer.playBeat(beat, bpm);
}

export function stop(): void {
  RnChordsPlayer.stop();
}

export const eventEmitter = new NativeEventEmitter(RnChordsPlayer);

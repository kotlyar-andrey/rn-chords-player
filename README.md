# rn-chords-player (Android only)

Chords player for react native

## Installation

```sh
npm install https://github.com/kotlyar-andrey/rn-chords-player
```

## Usage

```js
import { playBeat, stop, Beat } from 'rn-chords-player';

// ...
const beat1: Beat = {
  strikes: ['down', 'up', 'x', 'up'],
  duration: 2,
};
const bpm: number = 120;
// ...
onPress={() => {
  playBeat(beat1, bpm);
}}
```

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)

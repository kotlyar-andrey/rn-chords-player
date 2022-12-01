import * as React from 'react';

import { StyleSheet, View, TouchableOpacity, Text } from 'react-native';
import { playBeat, stop, Beat, eventEmitter } from 'rn-chords-player';

const beat1: Beat = {
  strikes: ['down', 'up', 'x', 'up', 'p'],
  duration: 2,
};
const beat2: Beat = {
  strikes: ['down', 'pause', 'x', 'up', 'pause', 'up', 'x', 'up'],
  duration: 3,
};

export default function App() {
  React.useEffect(() => {
    const eventListener = eventEmitter.addListener('Strike', (event) => {
      console.log('Strike: ', event.strikeIndex);
    });

    return () => {
      eventListener.remove();
    };
  }, []);

  return (
    <View style={styles.container}>
      <TouchableOpacity
        style={styles.box}
        onPress={() => {
          playBeat(beat1, 120);
        }}
      >
        <Text>Beat 1</Text>
      </TouchableOpacity>
      <TouchableOpacity
        style={styles.box}
        onPress={() => {
          playBeat(beat2, 150);
        }}
      >
        <Text>Beat 2</Text>
      </TouchableOpacity>
      <TouchableOpacity
        style={styles.box}
        onPress={() => {
          stop();
        }}
      >
        <Text>Stop</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    marginVertical: 20,
    backgroundColor: '#c0c0c0',
    padding: 10,
  },
});

import React, { useState } from 'react';
import { View, Button, Image, StyleSheet, NativeSyntheticEvent, NativeTouchEvent } from 'react-native';
import { NativeModules } from 'react-native';

const { CustomCamera } = NativeModules;

const CameraScreen: React.FC = () => {
    const [photo, setPhoto] = useState<string | null>(null);
    const [flash, setFlash] = useState<boolean>(false);

    const openCamera = async () => {
        try {
            const result: string = await CustomCamera.openCamera();
            setPhoto(`data:image/jpeg;base64,${result}`);
        } catch (error) {
            console.error(error);
        }
    };

    const toggleFlash = async () => {
        try {
            await CustomCamera.toggleFlash();
            setFlash((prevFlash) => !prevFlash);
        } catch (error) {
            console.error(error);
        }
    };

    return (
        <View style={styles.container}>
            <Button title="Open Camera" onPress={openCamera} />
            <Button title={`Flash ${flash ? 'On' : 'Off'}`} onPress={toggleFlash} />
            {photo && <Image source={{ uri: photo }} style={styles.photo} />}
        </View>
    );
};

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    photo: {
        marginTop: 20,
        width: 300,
        height: 300,
    },
});

export default CameraScreen;
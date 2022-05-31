package com.bnnthang.fltestbed.androidclient;

import com.bnnthang.fltestbed.commonutils.clients.IClientLocalRepository;
import com.bnnthang.fltestbed.commonutils.models.PowerConsumptionFromBytes;
import com.bnnthang.fltestbed.commonutils.utils.SocketUtils;

import org.apache.commons.lang3.SerializationUtils;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class AndroidLocalRepository implements IClientLocalRepository {
//    private static final Logger _logger = LoggerFactory.getLogger(AndroidLocalRepository.class);

    private File localFileDir = null;

    private static final String DATASET_FILENAME = "dataset";

    private static final String MODEL_FILENAME = "model.zip";

    public AndroidLocalRepository(File _localFileDir) {
        localFileDir = _localFileDir;
    }

    @Override
    public Long downloadModel(Socket socket) throws IOException {
        File datasetFile = new File(localFileDir, MODEL_FILENAME);
        datasetFile.createNewFile();

        FileOutputStream fos = new FileOutputStream(datasetFile);
        Long readBytes = SocketUtils.readAndSaveBytes(socket, fos);
        fos.close();

        return readBytes;
    }

    @Override
    public Long downloadDataset(Socket socket) throws IOException {
        File datasetFile = new File(localFileDir, DATASET_FILENAME);

        // create file if not exists
        datasetFile.createNewFile();

        // download and write to model file
        FileOutputStream datasetFileOutputStream = new FileOutputStream(datasetFile);
        Long readBytes = SocketUtils.readAndSaveBytes(socket, datasetFileOutputStream);
        datasetFileOutputStream.close();

        return readBytes;
    }

    @Override
    public Long updateModel(Socket socket) throws IOException {
        byte[] bytes = SocketUtils.readBytesWrapper(socket);

//        _logger.debug("recv weights length = " + bytes.length);
        System.err.println("recv weights length = " + bytes.length);

        INDArray params = SerializationUtils.deserialize(bytes);
        MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(new File(localFileDir, MODEL_FILENAME));
        model.setParams(params);
        model.save(new File(localFileDir, MODEL_FILENAME), true);
        model.close();

        return (long) bytes.length;
    }

    @Override
    public Long downloadModel(Socket socket, PowerConsumptionFromBytes powerConsumptionFromBytes) throws IOException {
        Long readBytes = downloadModel(socket);
        powerConsumptionFromBytes.increasePowerConsumption(readBytes);
        return readBytes;
    }

    @Override
    public Long downloadDataset(Socket socket, PowerConsumptionFromBytes powerConsumptionFromBytes) throws IOException {
        Long readBytes = downloadDataset(socket);
        powerConsumptionFromBytes.increasePowerConsumption(readBytes);
        return readBytes;
    }

    @Override
    public Long updateModel(Socket socket, PowerConsumptionFromBytes powerConsumptionFromBytes) throws IOException {
        Long readBytes = updateModel(socket);
        powerConsumptionFromBytes.increasePowerConsumption(readBytes);
        return readBytes;
    }

    @Override
    public Boolean modelExists() {
        return (new File(localFileDir, MODEL_FILENAME)).exists();
    }

    @Override
    public MultiLayerNetwork loadModel() throws IOException {
        return ModelSerializer.restoreMultiLayerNetwork(new File(localFileDir, MODEL_FILENAME));
    }

    @Override
    public Boolean datasetExists() {
        return (new File(localFileDir, DATASET_FILENAME)).exists();
    }

    @Override
    public Long getDatasetSize() throws IOException {
        if (datasetExists()) {
            return (new File(localFileDir, DATASET_FILENAME)).length();
        } else {
            throw new FileNotFoundException("file does not exist");
        }
    }

    @Override
    public InputStream getDatasetInputStream() throws IOException {
        return new FileInputStream(new File(localFileDir, DATASET_FILENAME));
    }

    @Override
    public File getModelFile() throws IOException {
        return new File(localFileDir, MODEL_FILENAME);
    }

    @Override
    public String getModelPath() {
        return MODEL_FILENAME;
    }
}

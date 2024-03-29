package com.jietong.rfid.uhf.dao.impl;

import gnu.io.SerialPort;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import networks.service.SocketService;
import networks.service.impl.SocketServiceImpl;
import serialport.service.SerialPortService;
import serialport.service.impl.SerialPortServiceImpl;
import com.jietong.rfid.uhf.entity.AntStruct;
import com.jietong.rfid.uhf.entity.CMD;
import com.jietong.rfid.uhf.entity.FrequencyPoint;
import com.jietong.rfid.uhf.entity.Multichannel16_32Ant;
import com.jietong.rfid.uhf.entity.PACKAGE;
import com.jietong.rfid.uhf.entity.ReaderCard;
import com.jietong.rfid.uhf.entity.ReaderCardV2;
import com.jietong.rfid.uhf.entity.StopReaderCard;
import com.jietong.rfid.uhf.service.CallBack;
import com.jietong.rfid.uhf.service.CallBackStopReadCard;
import com.jietong.rfid.uhf.tool.BCC;
import com.jietong.rfid.uhf.tool.BitOperation;
import com.jietong.rfid.uhf.tool.ERROR;
import com.jietong.rfid.util.DataConvert;

public class Reader extends PACKAGE {
	
	 void setHost(Reader reader, String host, int baudRate) {
		if (null == reader) {
			return;
		}
		reader.isSerialPortConnect = false;
		reader.host = host;
		if (reader.isSerialPortConnect) {
			reader.port = 11520;// ZL����Ϊ230400
		} else {
			reader.port = 20058;
		}
	}

	 boolean connect(Reader reader) {
		if (null == reader) {
			return false;
		}
		boolean flag = false;
		if (reader.isSerialPortConnect) {
			try {
				SerialPortService serialPortService = new SerialPortServiceImpl();
				reader.serialPort = serialPortService.open(reader.host, reader.port);
				if (null != reader.serialPort) {
					flag = true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else{
			try {
				
				SocketService networkService = new SocketServiceImpl();
				reader.socket = networkService.open(reader.host, reader.port);
				if (null != reader.socket) {
					flag = true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		reader.headCount = 0;
		reader.dataCount = 0;
		return flag;
	}

	 boolean disconnect(Reader reader) {
		if (null == reader) {
			return false;
		}
		if (reader.deviceConnected) {
			if (reader.isSerialPortConnect) {
				SerialPortService serialPortService = new SerialPortServiceImpl();
				serialPortService.close(reader.serialPort);
				reader.serialPort = null;
				reader.deviceConnected = false;
				return true;
			}else{
				SocketService networkService = new SocketServiceImpl();
				networkService.close(reader.socket);
				reader.socket = null;
				reader.deviceConnected = false;
				return true;
			}
		}
		return false;
	}

	 boolean socketSendData(Socket socket, byte[] sendData) {
		SocketService networkService = new SocketServiceImpl();
		return networkService.send(socket, sendData);
	}
	 
	void pintReceiveData(byte[] receiveData) {
		System.out.println("receive data : " + DataConvert.bytesToHexString(receiveData));
	}

	void pintSendData(byte[] sendData) {
		System.out.println("\n");
		System.out.print("send cmd : ");
		for (int i = 0; i < sendData.length; i++) {
			String data = DataConvert.bytesToHexString(sendData[i]);
			System.out.print(data + " ");
		}
		System.out.println();
	}

	 byte[] socketRecvData(Reader reader) {
		if (null == reader) {
			return null;
		}
		SocketService networkService = new SocketServiceImpl();
		byte [] result = networkService.read(reader.socket);
		return result;
	}
	 
	 boolean version(Reader reader, ByteBuffer buffer) {
		if (reader == null) {
			return false;
		}
		if (sendData(reader, CMD.UHF_GET_VERSION, null, 0)) {
			if (readData(reader, CMD.UHF_GET_VERSION, buffer,CMD.VERSION_LENGTH)) {
				if (compareStartCode(reader)) {
					return true;
				}
			}
		}
		return false;
	}

	 boolean getBuzzer(Reader reader, ByteBuffer buffer) {
		if (null == reader) {
			return false;
		}
		if (sendData(reader, CMD.UHF_GET_BUZZ, null, 0)) {
			if (readData(reader, CMD.UHF_GET_BUZZ, buffer, 0)) {
				if (reader.data[0] != ERROR.HOST_ERROR) {
					return true;
				}
			}
		}
		return false;
	}

	 boolean getWorkMode(Reader reader, ByteBuffer workMode) {
		if (null == reader) {
			return false;
		}
		ByteBuffer buffer = ByteBuffer.allocate(1);
		if (sendData(reader, CMD.UHF_GET_MODE, null, 0)) {
			if (readData(reader, CMD.UHF_GET_MODE, buffer, 1)) {
				workMode.put(buffer.array()[0]);
				return true;
			}
		}
		return false;
	}

	 boolean getTrigModeDelayTime(Reader reader, ByteBuffer trigTime) {
		if (null == reader) {
			return false;
		}
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_GET_TRIGGER_TIME, null, 0)) {
			if (readData(reader, CMD.UHF_GET_TRIGGER_TIME, buffer, 1)) {
				trigTime.put(buffer.array()[0]);
				return true;
			}
		}
		return false;
	}

	 boolean getNeighJudge(Reader reader, ByteBuffer enableNJ,
			ByteBuffer neighJudgeTime) {
		if (null == reader) {
			return false;
		}
		ByteBuffer buffer = ByteBuffer.allocate(2);
		if (sendData(reader, CMD.UHF_GET_TAG_FILTER, null, 0)) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (readData(reader, CMD.UHF_GET_TAG_FILTER, buffer, 2)) {
				enableNJ.put(buffer.array()[0]);
				neighJudgeTime.put(buffer.array()[1]);
				return true;
			}
		}
		return false;
	}

	 boolean getDeviceNo(Reader reader, ByteBuffer deviceNo) {
		if (null == reader) {
			return false;
		}
		if (sendData(reader, CMD.UHF_GET_DEVICE_NO, null, 0)) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (readData(reader, CMD.UHF_GET_DEVICE_NO, deviceNo, 1)) {
				if (reader.data[0] != ERROR.HOST_ERROR) {
					return true;
				}
			}
		}
		return false;
	}

	 boolean setDeviceNo(Reader reader, byte deviceNo) {
		if (null == reader) {
			return false;
		}
		byte buf[] = new byte[16];
		int bufsize = 1;
		buf[0] = deviceNo;
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_SET_DEVICE_NO, buf, bufsize)) {
			if (readData(reader, CMD.UHF_SET_DEVICE_NO, buffer, 1)) {
				if (reader.data[0] != ERROR.HOST_ERROR) {
					return true;
				}
			}
		}
		return false;
	}

	 boolean getOutputMode(Reader reader, ByteBuffer outputMode) {
		if (null == reader) {
			return false;
		}
		ByteBuffer buffer = ByteBuffer.allocate(16);
		if (sendData(reader, CMD.UHF_GET_OUTPUT, null, 0)) {
			if (readData(reader, CMD.UHF_GET_OUTPUT, buffer, 1)) {
				outputMode.put(buffer.array()[0]);
				return true;
			}
		}
		return false;
	}

	 boolean getRelayAutoState(Reader reader, ByteBuffer state) {
		if (reader == null) {
			return false;
		}
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_GET_TRIGGER_TIME, null, 0)) {
			if (readData(reader, CMD.UHF_GET_TRIGGER_TIME, buffer, 1)) {
				state.put(buffer.array()[0]);
				return true;
			}
		}
		return false;
	}
	 
	boolean readTagData(Reader reader, byte bank, byte begin, byte size, ByteBuffer getBuffer, byte[] password) {
		if (null == reader) {
			return false;
		}
		if (getBuffer.limit() < 1) {
			return false;
		}
		if (bank == 0) {// ������
			if (begin + size > 4) {
				return false;
			}
		} else if (bank == 1) { // EPC��
			if (begin + size > 8) {
				return false;
			}
		} else if (bank == 2) { // TID��
			if (begin + size > 6) {
				return false;
			}
		} else if (bank == 3) { // �û���
			if (begin + size > 32) {
				return false;
			}
		} else { // ��Ч��bankֵ
			return false;
		}
		byte sendBuf[] = new byte[256];
		int bufsize = 7;
		sendBuf[0] = (byte) bank;
		sendBuf[1] = (byte) begin;
		sendBuf[2] = (byte) size;
		System.arraycopy(password, 0, sendBuf, 3, 4);
		if (sendData(reader, CMD.UHF_READ_TAG_DATA, sendBuf, bufsize)) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// �����������ݴ�ŵ�buffer
			ByteBuffer buffer = ByteBuffer.allocate(20);
			if (readData(reader, CMD.UHF_READ_TAG_DATA, buffer, size * 2)) {
				if (reader.data[0] != ERROR.HOST_ERROR) {
					byte[] data = reader.data;
					for (int i = 0; i < data.length; i++) {
						getBuffer.put(data[i]);
					}
					return true;
				}
				return false;
			}
		}
		return false;
	}
	
	boolean comSendData(SerialPort serialPort, byte[] sendData) {
		//pintSendData(sendData);
		SerialPortService serialPortService = new SerialPortServiceImpl();
		return serialPortService.send(serialPort, sendData);
	}

	boolean sendData(Reader reader, byte cmd, byte[] sendBuf, int bufsize) {
		if (null == reader) {
			return false;
		}
		startcode[0] = CMD.NET_START_CODE1;
		startcode[1] = CMD.NET_START_CODE2;
		this.cmd = cmd;
		seq = 0;
		len[0] = (byte) bufsize;
		len[1] = (byte) (bufsize / 256);
		bcc = 0;
		if (bufsize > 0) {
			data = Arrays.copyOf(sendBuf, bufsize + 1);
			bcc = BCC.checkSum(sendBuf, bufsize);
		} else {
			data = Arrays.copyOf(data, 1);
		}
		data[bufsize] = bcc;
		byte[] receiveData = getSendCMD(bufsize);
		boolean size = false;
		if (isSerialPortConnect) {
			size = comSendData(serialPort, receiveData);
		} else {
			size = socketSendData(socket,receiveData);
		}
		return size;
	}


	
	byte[] comReceiveData(Reader reader) {
		if (null == reader) {
			return null;
		}
		SerialPortService serialPortService = new SerialPortServiceImpl();
		byte[] result = serialPortService.read(serialPort);
		return result;
	}
	
	boolean trandPackage(Reader reader, byte data, ByteBuffer buffer,
			ByteBuffer returnLength) {
		if (null == reader) {
			return false;
		}
		if (headCount < CMD.HEAD_LENGTH) {
			switch (headCount) {
			case 0:
				if (data == CMD.NET_START_CODE1) {
					headCount++;
				}
				break;
			case 1:
				if (data == CMD.NET_START_CODE2) {
					headCount++;
				}
				break;
			case 2:
				cmd = data;
				headCount++;
				break;
			case 3:
				seq = data;
				headCount++;
				break;
			case 4:
				receiveLength = DataConvert.byteToInt(data);
				len[0] = data;
				buffer.clear();
				returnLength.clear();
				returnLength.put(data);
				headCount++;
				break;
			case 5:
				len[1] = data;
				headCount++;
				break;
			}
		} else if (dataCount < receiveLength) {
			buffer.put(data);
			dataCount++;
		} else {
			byte[] bufData = Arrays.copyOf(buffer.array(), receiveLength);
			bcc = BCC.checkSum(bufData, returnLength.array()[0]);
			if (bcc == data) {
				headCount = 0;
				dataCount = 0;
				return true;
			} else {
				headCount = 0;
				dataCount = 0;
				return false;
			}
		}
		return false;
	}
	
	boolean readData(Reader reader, byte cmd, ByteBuffer buffer, int length) {
		if (null == reader) {
			return false;
		}
		boolean flag = false;
		long begin = System.currentTimeMillis();
		long timeout = 1000;
		boolean once = false;
		byte[] receiveVal = null;
		while (deviceConnected) {
			long end = System.currentTimeMillis();
			if (end - begin > timeout) {
				// return flag;
			}
			if (once) {
				return flag;
			}
			once = true;
			if (isSerialPortConnect) {
				receiveVal = comReceiveData(reader);
			} else {
				receiveVal = socketRecvData(reader);
			}
			if(null == receiveVal){
				flag = false;
				return false;
			}
			ByteBuffer receiveBuf = ByteBuffer.allocate(50);
			ByteBuffer receiveLength = ByteBuffer.allocate(1);

			for (int i = 0; i < receiveVal.length; i++) {
				if (trandPackage(reader, receiveVal[i], receiveBuf,receiveLength)) {
					if (this.cmd == cmd) {
						int _length = DataConvert.byteToInt(receiveLength.array()[0]);
						byte[] _readData = Arrays.copyOf(receiveBuf.array(),_length);
						if (null != buffer && buffer.limit() > 0) {
							buffer.put(_readData);// ȥ�����ӵ����ݳ���
						}
						flag = true;
					}
				}
			}
		}
		return flag;
	}

	boolean compareStartCode(Reader reader) {
		if (null == reader) {
			return false;
		}
		if (startcode[0] == CMD.NET_START_CODE1
				&& startcode[1] == CMD.NET_START_CODE2) {
			return true;
		}
		return false;
	}

	String version(Reader reader) {
		if (reader == null) {
			return null;
		}
		ByteBuffer buffer = ByteBuffer.allocate(16);
		if (sendData(reader, CMD.UHF_GET_VERSION, null, 0)) {
			if (readData(reader, CMD.UHF_GET_VERSION, buffer,CMD.VERSION_LENGTH)) {
				if (compareStartCode(reader)) {
					String ver = new String(buffer.array()).trim();
					reader.setVersion(ver);
					return ver;
				}
			}
		}
		return null;
	}

	boolean getAnt(Reader reader, ByteBuffer buffer) {
		if (null == reader) {
			return false;
		}
		if (sendData(reader, CMD.UHF_GET_ANT_CONFIG, null, 0)) {
			if (readData(reader, CMD.UHF_GET_ANT_CONFIG, buffer,CMD.ANT_CFG_LENGTH)) {
				if (compareStartCode(reader)) {
					return true;
				}
			}
		}
		return false;
	}

	@Deprecated
	boolean invOnce(Reader reader, CallBack callBack) {
		if (null == reader) {
			return false;
		}
		if (!deviceConnected) {
			return false;
		}
		// һ��Ѱ��ʱ����ȡ���߹���״̬, ���ڿ����̺߳�ʱ����
		ByteBuffer buffer = ByteBuffer.allocate(100);
		boolean getAnt = getAnt(reader, buffer);
		if (!getAnt) {
			return false;
		}
		// ��������״̬��Ϊ0,��ȴ��������������Ѱ��
		threadStart = false;
		headCount = 0;
		dataCount = 0;
		if (sendData(reader, CMD.UHF_INV_ONCE, null, 0)) {
			threadStart = true;
			ReaderCard readerCard = new ReaderCard(reader, callBack);
			Thread thread = new Thread(readerCard);
			thread.start();
			return true;
		}
		return false;
	}

	boolean invOnceV2(Reader reader, CallBack callBack) {
		if (null == reader) {
			return false;
		}
		if (!deviceConnected) {
			return false;
		}
		// һ��Ѱ��ʱ����ȡ���߹���״̬, ���ڿ����̺߳�ʱ����
		ByteBuffer buffer = ByteBuffer.allocate(100);
		boolean getAnt = getAnt(reader, buffer);
		if (!getAnt) {
			return false;
		}
		// ��������״̬��Ϊ0,��ȴ��������������Ѱ��
		threadStart = false;
		headCount = 0;
		dataCount = 0;
		if (sendData(reader, CMD.UHF_INV_ONCE_V2, null, 0)) {
			threadStart = true;
			ReaderCardV2 readerCard = new ReaderCardV2(reader, callBack);
			Thread thread = new Thread(readerCard);
			thread.start();
			return true;
		}
		return false;
	}

	@Deprecated
	boolean beginInv(Reader reader, CallBack callBack) {
		if (null == reader) {
			return false;
		}
		threadStart = false;
		stopRead = false;
		boolean ret = false;
		if (!deviceConnected) {
			return false;
		}
		headCount = 0;
		dataCount = 0;
		if (sendData(reader, CMD.UHF_INV_MULTIPLY_BEGIN, null, 0)) {
			threadStart = true;
			ReaderCard readThread = new ReaderCard(reader, callBack);
			Thread loopThread = new Thread(readThread);
			loopThread.start();
			ret = true;
		} else {
			ret = false;
		}
		return ret;
	}

	boolean beginInvV2(Reader reader, CallBack callBack) {
		if (null == reader) {
			return false;
		}
		if(threadStart){
			return false;
		}
		stopRead = false;
		boolean ret = false;
		if (!deviceConnected) {
			return false;
		}
		headCount = 0;
		dataCount = 0;
		if (sendData(reader, CMD.UHF_INV_MULTIPLY_BEGIN_V2, null, 0)) {
			threadStart = true;
			ReaderCardV2 readThread = new ReaderCardV2(reader, callBack);
			Thread loopThread = new Thread(readThread);
			loopThread.start();
			ret = true;
		} else {
			ret = false;
		}
		return ret;
	}

	@Deprecated
	boolean stopInv(Reader reader, CallBackStopReadCard callBackStopReadCard) {
		if (reader == null) {
			return false;
		}
		if (!threadStart) {
			return true;
		}
		threadStart = false; // �����߳̽�����־
		// ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_INV_MULTIPLY_END, null, 0)) {
			StopReaderCard stopReaderCard = new StopReaderCard(reader,callBackStopReadCard);
			Thread thread = new Thread(stopReaderCard);
			thread.start();
			// if (readData(reader, CMD.UHF_INV_MULTIPLY_END, buffer, 1)) {
			// if (data[0] != ERROR.HOST_ERROR) {
			// return true;
			// }
			// }
		}
		return false;
	}

	boolean stopInvV2(Reader reader, CallBackStopReadCard callBackStopReadCard) {
		if (reader == null) {
			return false;
		}
		if (!threadStart) {
			return true;
		}
		threadStart = false; // �����߳̽�����־
		// ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_INV_MULTIPLY_END_V2, null, 0)) {
			StopReaderCard stopReaderCard = new StopReaderCard(reader,callBackStopReadCard);
			Thread thread = new Thread(stopReaderCard);
			thread.start();
			// if (readData(reader, CMD.UHF_INV_MULTIPLY_END_V2, buffer, 1)) {
			// if (data[0] != ERROR.HOST_ERROR) {
			// return true;
			// }
			// }
		}
		return false;
	}

	byte[] deviceReadBuffer(Reader reader) {
		if (null == reader) {
			return null;
		}
		byte[] buffer = null;
		if (null != reader) {
			if (isSerialPortConnect) {
				buffer = comReceiveData(reader);
			} else {
				buffer = socketRecvData(reader);// ����1�볬ʱ
			}
		}
		return buffer;
	}

	public Map<String,String> filterEpcAndAnt(byte[] readData) {
		Map<String,String> map = null;
		if (readData.length == 16) {
			map = new HashMap<String,String>();
			String total = DataConvert.bytesToHexString(readData);
			String antten = total.substring(total.length() - 2, total.length());
			int ant = Integer.parseInt(antten, 16);
			String EPC = DataConvert.bytesToHexString(Arrays.copyOf(readData,12));
			map.put("EPC", EPC);
			map.put("ANT", String.valueOf(ant + 1));
		}
		return map;
	}
	
	public Map<String,String> filterEpcRssiAndAnt(byte[] readData) {
		Map<String,String> filterData = new HashMap<String,String>();
		filterData = new HashMap<String, String>();
		byte[] dataStorage = new byte[5];
		
		String ant = ""; //dataStorage[0]
		String rssi = "";//dataStorage[1]
		String deviceNo = "";//dataStorage[2,3]
		String direction = "";//dataStorage[4]
		
        //String data = DataConvert.bytesToHexString(readData);
        
		
		System.arraycopy(readData, 0, dataStorage, 0, 1);
		int EPCLength = DataConvert.byteToInt(dataStorage[0]);//temporary storage
		
		System.arraycopy(readData, 1, dataStorage, 0, 1);
		boolean[] type = BitOperation.byteToBooleans(dataStorage[0]);//temporary storage
		
		int index = 0;
		if (type[0]) { // ������
			System.arraycopy(readData,EPCLength + 2 + index , dataStorage, 0, 1);
			ant = String.valueOf(DataConvert.byteToInt(dataStorage[0]));
			index++;
		}
		if (type[1]) {// ��rssi
			System.arraycopy(readData, EPCLength + 2 + index, dataStorage, 1, 1);
			rssi = String.valueOf(DataConvert.byteToInt(dataStorage[1]));
			index++;
		}
		if (type[2]) {// ���豸��  
			System.arraycopy(readData, EPCLength + 2 + index, dataStorage, 2, 2);
			int devNo = 0;
			devNo|=DataConvert.byteToInt(dataStorage[2])<<8;
			devNo|=DataConvert.byteToInt(dataStorage[3]);
			deviceNo = String.valueOf(devNo);
			index+=2;
		}
		if (type[3]) {// �д�������
			System.arraycopy(readData, EPCLength + 2 + index, dataStorage, 4, 1);
			direction = String.valueOf(DataConvert.byteToInt(dataStorage[4]));
			index++; //Ԥ��
		}
		byte[] epcData = new byte[EPCLength];
		System.arraycopy(readData, 2, epcData, 0, EPCLength);
		String EPC = DataConvert.bytesToHexString(epcData);
		
		filterData.put("EPC", EPC); //EPC
		filterData.put("ANT", ant); // ����
		filterData.put("RSSI", rssi);// RSSI
		filterData.put("DeviceNo", deviceNo);// deviceNo
		filterData.put("Direction", direction);// direction
		
		return filterData;
	}

	void deviceTransBuffer(Reader reader, byte[] buffer, CallBack callBack) {
		if (null == reader) {
			return;
		}
		ByteBuffer receiveBuf = ByteBuffer.allocate(50);
		ByteBuffer receiveLength = ByteBuffer.allocate(1);
		Map<String,String> map = null;
		for (int i = 0; i < buffer.length; i++) {
			if (trandPackage(reader, buffer[i], receiveBuf, receiveLength)) {
				int length = DataConvert.byteToInt(receiveLength.array()[0]);
				byte[] readData = Arrays.copyOf(receiveBuf.array(), length);
				switch (cmd) {
				case 0x25:// Ѱ��һ��
					if (length == 16) {
						map = filterEpcAndAnt(readData);
						//callBack.readData(map.get("EPC"), map.get("ANT"));
					}
					// ����ǲ��ǽ�����
					if (2 == length) {// ĳ����Ѱ���������ݰ�
						String data = DataConvert.bytesToHexString(readData[1]);
						if (data.equals("F0")) {
							threadStart = false; // �����߳̽�����־
						}
					}
					break;
				case 0x2A:// ����Ѱ��ģʽ����������
					if (length == 16) {
						map = filterEpcAndAnt(readData);
						//callBack.readData(map.get("EPC"), map.get("ANT"));
					}
					break;
				case 0x2B:// ֹͣ����Ѱ��ģʽ
					if (readData[0] != ERROR.HOST_ERROR) {
						stopRead = true;
					}
				default:
					break;
				}
			}
		}
	}
	
	
	void deviceTransBufferV2(Reader reader, byte[] buffer, CallBack callBack) {
		if (null == reader) {
			return;
		}
		ByteBuffer receiveBuf = ByteBuffer.allocate(50);
		ByteBuffer receiveLength = ByteBuffer.allocate(1);
		Map<String,String> EpcData = null;
		for (int i = 0; i < buffer.length; i++) {
			if (trandPackageV2(reader, buffer[i], receiveBuf, receiveLength)) {
				int length = DataConvert.byteToInt(receiveLength.array()[0]);
				byte[] readData = Arrays.copyOf(receiveBuf.array(), length);
				// PRINT ACA
				//2018-11-30������ Э��
				switch (cmd) {
				case (byte) 0xE5:// Ѱ��һ��
					if (length > 10) {
						// RECUPERA EPC
						EpcData = filterEpcRssiAndAnt(readData);
						callBack.readData(EpcData.get("EPC"), EpcData.get("RSSI"), EpcData.get("ANT"),EpcData.get("DeviceNo"),EpcData.get("Direction"));
					}
					if(seq == 0x02){
						if(1 == length){
							threadStart = false; // �����߳̽�����־
						}
					}
					break;
				case (byte) 0xEA:// ����Ѱ��ģʽ����������
					if (length > 10) {
						EpcData = filterEpcRssiAndAnt(readData);
						callBack.readData(EpcData.get("EPC"), EpcData.get("RSSI"), EpcData.get("ANT"),EpcData.get("DeviceNo"),EpcData.get("Direction"));
					}
					break;
				case (byte) 0xEB:// ֹͣ����Ѱ��ģʽ
					if (readData[0] != ERROR.HOST_ERROR) {
						stopRead = true;
					}
					break;
				default:
					break;
				}
			}
		}
	}

	byte[] setAnt(AntStruct antStruct) {
		if (antStruct.state == 4) {
			return setAnt4(antStruct);
		} else if (antStruct.state == 32) {
			return new Multichannel16_32Ant().setAnt32(antStruct);
		} else if (antStruct.state == 16) {
			return new Multichannel16_32Ant().setAnt16(antStruct);
		}
		return null;
	}

	byte[] setAnt4(AntStruct antStruct) {
		ByteBuffer sendAnt = ByteBuffer.allocate(36);
		byte[] antenner = new byte[4];
		for (int i = 0; i < antenner.length; i++) {
			antenner[i] = antStruct.enable[i];
		}
		sendAnt.put(antenner);
		byte[] time = new byte[4];
		for (int i = 0; i < time.length; i++) {
			time = DataConvert.intToByteArray(antStruct.dwellTime[i]);
			sendAnt.put(time);
		}
		byte[] power = new byte[4];
		for (int i = 0; i < power.length; i++) {
			power = DataConvert.intToByteArray(antStruct.power[i]);
			sendAnt.put(power);
		}
		return sendAnt.array();
	}

	boolean setAnt(Reader reader, AntStruct ant) {
		if (null == reader) {
			return false;
		}
		ByteBuffer buffer = ByteBuffer.allocate(100);
		byte[] sendAntData = setAnt(ant);
		if (null == sendAntData) {
			return false;
		}
		if (sendData(reader, CMD.UHF_SET_ANT_CONFIG, sendAntData,
				CMD.ANT_CFG_LENGTH)) {
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (readData(reader, CMD.UHF_SET_ANT_CONFIG, buffer, 1)) {
				if (data[0] != ERROR.HOST_ERROR) {
					return true;
				}
			}
		}
		return false;
	}

	public void threadFunc(final Reader reader, final CallBack callBack) {
		if (null == reader) {
			return;
		}
		boolean exit = true;
		do {
			final byte[] buffer = deviceReadBuffer(reader);
			if (null != buffer) {
				deviceTransBuffer(reader, buffer, callBack);
			}
			if (!threadStart) {
				if (null == buffer) {
					exit = threadStart;
				}
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (exit);
		// �����߳���Դ
		receiveLength = 0;
		headCount = 0;
		dataCount = 0;
	}
	
	public void threadFuncV2(final Reader reader, final CallBack callBack) {
		if (null == reader) {
			return;
		}
		boolean exit = true;
		/*byte[] pwd = new byte[4];
		for (int iv = 0; iv < 4; ++iv) {
			String str = "00000000".substring(iv * 2, (2 + iv * 2));
			pwd[iv] = Byte.parseByte(str, 16);
		}*/
		do {
			try {
				byte[] buffer = deviceReadBuffer(reader);
				
				/*String TID =  readTagData(reader, (byte) 2, (byte) 0, (byte) 6, pwd);
				if (! TID.equals("800000000000000000000000")) {
					System.out.print("TID: " + TID);
				}*/

				if (null != buffer) {
					deviceTransBufferV2(reader, buffer, callBack);
				}
				if (!threadStart) {
					if (null == buffer) {
						exit = threadStart;
					}
				}
				Thread.sleep(1);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} while (exit);
		// �����߳���Դ
		receiveLength = 0;
		headCount = 0;
		dataCount = 0;
	}

	boolean writeTagData(Reader reader, int bank, int begin, int length,String data, byte[] password) {
		if (null == reader) {
			return false;
		}
		byte buf[] = new byte[256];
		int bufsize = 3 + length * 2 + 4;// length����
		buf[0] = (byte) bank;
		buf[1] = (byte) begin;
		buf[2] = (byte) length;
		System.arraycopy(password, 0, buf, 3, 4);
		byte[] inData = new byte[data.length() / 2];
		int count = 0;
		for (int i = 0; i < inData.length; i++) {
			int result = Integer.parseInt(data.substring(count, count + 2), 16);
			count += 2;
			inData[i] = (byte) result;
		}
		System.arraycopy(inData, 0, buf, 3 + 4, length * 2);// Ҫд�������
		ByteBuffer buffer = ByteBuffer.allocate(20);
		if (sendData(reader, CMD.UHF_WRITE_TAG_DATA, buf, bufsize)) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (readData(reader, CMD.UHF_WRITE_TAG_DATA, buffer, 1)) {
				if (this.data[0] != ERROR.HOST_ERROR) {
					return true;
				}
			}
		}
		return false;
	}

	boolean lockTag(Reader reader, byte locktType, byte lockBank,
			byte[] password) {
		if (null == reader) {
			return false;
		}
		if (!deviceConnected) {
			return false;
		}
		if (password.length < 1) {
			return false;
		}
		if (locktType < 0 || locktType > 3) {
			return false;
		}
		if (lockBank < 0 || lockBank > 4) {
			return false;
		}
		byte buf[] = new byte[12];
		buf[0] = locktType;
		buf[1] = lockBank;
		System.arraycopy(password, 0, buf, 2, password.length);
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_LOCK_TAG, buf, 6)) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (readData(reader, CMD.UHF_LOCK_TAG, buffer, 1)) {
				if (data[0] != ERROR.HOST_ERROR) {
					return true;
				}
			}
		}
		return false;
	}

	int getBuzzer(Reader reader) {
		if (null == reader) {
			return -1;
		}
		ByteBuffer buffer = ByteBuffer.allocate(10);
		if (sendData(reader, CMD.UHF_GET_BUZZ, null, 0)) {
			if (readData(reader, CMD.UHF_GET_BUZZ, buffer, 0)) {
				if (data[0] != ERROR.HOST_ERROR) {
					return DataConvert.byteToInt(buffer.array()[0]);
				}
			}
		}
		return -1;
	}

	boolean setBuzzer(Reader reader, byte buzz) {
		if (null == reader) {
			return false;
		}
		byte[] buf = new byte[2];
		buf[0] = buzz;
		ByteBuffer buffer = ByteBuffer.allocate(10);
		if (sendData(reader, CMD.UHF_SET_BUZZ, buf, 1)) {
			if (readData(reader, CMD.UHF_SET_BUZZ, buffer, 0)) {
				if (data[0] != ERROR.HOST_ERROR) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * δ����
	 * @param reader
	 * @param buffer
	 * @return
	 */
	boolean getDI(Reader reader, ByteBuffer buffer) {
		if (null == reader) {
			return false;
		}
		if (sendData(reader, CMD.UHF_GET_DI_STATE, null, 0)) {
			if (readData(reader, CMD.UHF_GET_DI_STATE, buffer, 2)) {
				return true;
			}
		}
		return false;
	}

	boolean setWorkMode(Reader reader, int mode) {
		if (null == reader) {
			return false;
		}
		byte buf[] = new byte[8];
		int bufsize = 1;
		buf[0] = (byte) mode;
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_SET_MODE, buf, bufsize)) {
			if (readData(reader, CMD.UHF_SET_MODE, buffer, 1)) {
				if (data[0] != ERROR.HOST_ERROR) {
					return true;
				}
			}
		}
		return false;
	}

	int getWorkMode(Reader reader) {
		if (null == reader) {
			return -1;
		}
		ByteBuffer buffer = ByteBuffer.allocate(1);
		if (sendData(reader, CMD.UHF_GET_MODE, null, 0)) {
			if (readData(reader, CMD.UHF_GET_MODE, buffer, 1)) {
				return DataConvert.byteToInt(buffer.array()[0]);
			}
		}
		return -1;
	}

	boolean setTrigModeDelayTime(Reader reader, byte trigTime) {
		if (null == reader) {
			return false;
		}
		byte buf[] = new byte[16];
		buf[0] = trigTime;
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_SET_TRIGGER_TIME, buf, 1)) {
			if (readData(reader, CMD.UHF_SET_TRIGGER_TIME, buffer, 1)) {
				return true;
			}
		}
		return false;
	}

	int getTrigModeDelayTime(Reader reader) {
		if (null == reader) {
			return -1;
		}
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_GET_TRIGGER_TIME, null, 0)) {
			if (readData(reader, CMD.UHF_GET_TRIGGER_TIME, buffer, 1)) {
				return DataConvert.byteToInt(buffer.array()[0]);
			}
		}
		return -1;
	}

	Map<String,Byte> getNeighJudge(Reader reader) {
		if (null == reader) {
			return null;
		}
		ByteBuffer buffer = ByteBuffer.allocate(2);
		if (sendData(reader, CMD.UHF_GET_TAG_FILTER, null, 0)) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (readData(reader, CMD.UHF_GET_TAG_FILTER, buffer, 2)) {
				Map<String,Byte> map = new HashMap<String,Byte>();
				map.put("enableNJ", buffer.array()[0]);
				map.put("neighJudgeTime", buffer.array()[1]);
				return map;
			}
		}
		return null;
	}

	boolean setNeighJudge(Reader reader, byte neighJudgeTime) {
		if (null == reader) {
			return false;
		}
		int bufsize = 2;
		byte[] buf = new byte[16];
		buf[0] = (byte) (neighJudgeTime > 0 ? 1 : 0); // timeΪ0,
														// ȡ�������ж�����0�����������ж�
		buf[1] = neighJudgeTime;
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_SET_TAG_FILTER, buf, bufsize)) {
			if (readData(reader, CMD.UHF_SET_TAG_FILTER, buffer, 1)) {
				if (data[0] != ERROR.HOST_ERROR) {
					return true;
				}
			}
		}
		return false;
	}

	String getDeviceNo(Reader reader) {
		if (null == reader) {
			return null;
		}
		ByteBuffer buffer = ByteBuffer.allocate(2);
		if (sendData(reader, CMD.UHF_GET_DEVICE_NO, null, 0)) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (readData(reader, CMD.UHF_GET_DEVICE_NO, buffer, 1)) {
				if (data[0] != ERROR.HOST_ERROR) {
					int deviceNo = 0;
					deviceNo|=DataConvert.byteToInt(buffer.array()[0])<<8;
					deviceNo|=DataConvert.byteToInt(buffer.array()[1]);
					return String.valueOf(deviceNo);
				}
			}
		}
		return null;
	}

	boolean setDeviceNo(Reader reader, int deviceNo) {
		if (null == reader) {
			return false;
		}
		byte buf[] = new byte[16];
		int bufsize = 2;
		if(deviceNo < 0 || deviceNo > 65535){
			return false;
		}
		buf[0] = (byte) (deviceNo>>8);
		buf[1] = (byte) deviceNo;
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_SET_DEVICE_NO, buf, bufsize)) {
			if (readData(reader, CMD.UHF_SET_DEVICE_NO, buffer, 1)) {
				if (data[0] != ERROR.HOST_ERROR) {
					return true;
				}
			}
		}
		return false;
	}

	int getOutputMode(Reader reader) {
		if (null == reader) {
			return -1;
		}
		ByteBuffer buffer = ByteBuffer.allocate(16);
		if (sendData(reader, CMD.UHF_GET_OUTPUT, null, 0)) {
			if (readData(reader, CMD.UHF_GET_OUTPUT, buffer, 1)) {
				return DataConvert.byteToInt(buffer.array()[0]);
			}
		}
		return -1;
	}

	boolean setOutputMode(Reader reader, byte outputMode) {
		if (null == reader) {
			return false;
		}
		byte buf[] = new byte[16];
		buf[0] = outputMode;
		ByteBuffer buffer = ByteBuffer.allocate(16);
		if (sendData(reader, CMD.UHF_SET_OUTPUT, buf, 1)) {
			if (readData(reader, CMD.UHF_SET_OUTPUT, buffer, 1)) {
				return true;
			}
		}
		return false;
	}

	boolean killTag(Reader reader, byte[] accessPwd, byte[] killPwd) {
		if (null == reader) {
			return false;
		}
		byte buf[] = new byte[16];
		System.arraycopy(killPwd, 0, buf, 0, 4);
		System.arraycopy(accessPwd, 0, buf, 4, 4);
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_KILL_TAG, buf, 8)) {
			if (readData(reader, CMD.UHF_KILL_TAG, buffer, 1)) {
				if (data[0] != ERROR.HOST_ERROR) {
					return true;
				}
			}
		}
		return false;
	}

	int getRelayAutoState(Reader reader) {
		if (reader == null) {
			return -1;
		}
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_GET_TRIGGER_TIME, null, 0)) {
			if (readData(reader, CMD.UHF_GET_TRIGGER_TIME, buffer, 1)) {
				return DataConvert.byteToInt(buffer.array()[0]);
			}
		}
		return -1;
	}

	boolean setRelayAutoState(Reader reader, byte time) {
		if (reader == null) {
			return false;
		}
		byte buf[] = new byte[16];
		buf[0] = time;
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_SET_TRIGGER_TIME, buf, 1)) {
			if (readData(reader, CMD.UHF_SET_TRIGGER_TIME, buffer, 1)) {
				return true;
			}
		}
		return false;
	}

	boolean setDeviceConfig(Reader reader, byte[] para) {
		if (reader == null) {
			return false;
		}
		byte buf[] = new byte[128];
		byte bufSize = 20;
		System.arraycopy(para, 0, buf, 0, bufSize);
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_SET_CONFIGURE, buf, bufSize)) {
			if (readData(reader, CMD.UHF_SET_CONFIGURE, buffer, 1)) {
				return true;
			}
		}
		return false;
	}

	String readTagData(Reader reader, byte bank, byte begin, byte size,byte[] password) {
		if (null == reader) {
			return null;
		}
		int length = size * 2;
		if (length < 1) {
			return null;
		}
		if (bank == 0) {// ������
			if (begin + size > 4) {
				return null;
			}
		} else if (bank == 1) { // EPC��
			if (begin + size > 8) {
				return null;
			}
		} else if (bank == 2) { // TID��
			if (begin + size > 6) {
				return null;
			}
		} else if (bank == 3) { // �û���
			if (begin + size > 32) {
				return null;
			}
		} else { // ��Ч��bankֵ
			return null;
		}
		byte sendBuf[] = new byte[256];
		int bufsize = 7;
		sendBuf[0] = (byte) bank;
		sendBuf[1] = (byte) begin;
		sendBuf[2] = (byte) size;
		System.arraycopy(password, 0, sendBuf, 3, 4);
		if (sendData(reader, CMD.UHF_READ_TAG_DATA, sendBuf, bufsize)) {
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// �����������ݴ�ŵ�buffer
			ByteBuffer buffer = ByteBuffer.allocate(20);
			if (readData(reader, CMD.UHF_READ_TAG_DATA, buffer, length)) {
				if (data[0] != ERROR.HOST_ERROR) {
					byte[] total = Arrays.copyOf(buffer.array(),length);
					StringBuffer sb = new StringBuffer();
					for (int i = 0; i < length; i++) {
						sb.append(DataConvert.bytesToHexString(total[i]));						
					}
					//System.out.println("str " + sb.toString());
					return sb.toString();
				}
				return null;
			}
		}
		return null;
	}
	
	boolean trandPackageV2(Reader reader, byte data, ByteBuffer buffer,
			ByteBuffer returnLength) {
		if (null == reader) {
			return false;
		}
		if (headCount < CMD.HEAD_LENGTH) {
			switch (headCount) {
			case 0:
				if (data == CMD.NET_START_CODE1) {
					headCount++;
				}
				break;
			case 1:
				if (data == CMD.NET_START_CODE2) {
					headCount++;
				}
				break;
			case 2:
				cmd = data;
				headCount++;
				break;
			case 3:
				seq = data;
				headCount++;
				break;
			case 4:
				receiveLength = DataConvert.byteToInt(data);
				len[0] = data;
				buffer.clear();
				returnLength.clear();
				returnLength.put(data);
				headCount++;
				break;
			case 5:
				len[1] = data;
				headCount++;
				break;
			}
		} else if (dataCount < receiveLength) {
			buffer.put(data);
			dataCount++;
		} else {
			byte[] bufData = Arrays.copyOf(buffer.array(), receiveLength);
			bcc = BCC.checkSum(bufData, returnLength.array()[0]);
			if (bcc == data) {
				headCount = 0;
				dataCount = 0;
				return true;
			} else {
				headCount = 0;
				dataCount = 0;
				return false;
			}
		}
		return false;
	}
	
	/***********************2018-12-03 new add ******************************/
	
	public Map<String, Integer> getInvPatternConfig(Reader reader) {
		if (reader == null) {
			return null;
		}
		Map<String, Integer> config = null;
		byte buf[] = new byte[16];
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_GET_INV_PATTERN_CONFIG, buf, 1)) {
			if (readData(reader, CMD.UHF_GET_INV_PATTERN_CONFIG, buffer, 1)) {
				int length = 4;
				byte []  _buffer = Arrays.copyOf(buffer.array(), length);
				int [] value = new int[length];
				int i = 0;
				for (byte data : _buffer) {
					value[i] = DataConvert.byteToInt(data);
					i++;
				}
				config = new HashMap<String,Integer>();
				config.put("session", value[0]);
				config.put("qValue", value[1]);
				config.put("tagFocus",value[2]);
				config.put("abValue", value[3]);
				return config;
			}
		}
		return null;
	}

	public boolean setInvPatternConfig(Reader reader, byte session,byte qValue, byte tagFocus, byte abValue) {
		if (reader == null) {
			return false;
		}
		byte buf[] = new byte[16];
		buf[0] = session;
		buf[1] = qValue;
		buf[2] = tagFocus;
		buf[3] = abValue;
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_SET_INV_PATTERN_CONFIG, buf, 4)) {
			if (readData(reader, CMD.UHF_SET_INV_PATTERN_CONFIG, buffer, 1)) {
				return true;
			}
		}
		return false;
	}

	public Map<String, Boolean> getInvOutPutData(Reader reader) {
		if (reader == null) {
			return null;
		}
		Map<String, Boolean> config = null;
		byte buf[] = new byte[16];
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_GET_INV_OUTPUT_DATA, buf, 1)) {
			if (readData(reader, CMD.UHF_GET_INV_OUTPUT_DATA, buffer, 1)) {
				boolean [] value = BitOperation.byteToBooleans(buffer.array()[0]);
				config = new HashMap<String,Boolean>();
				config.put("antenna", value[0]);
				config.put("rssi", value[1]);
				config.put("deviceNo",value[2]);
				config.put("accessDoorDirection",value[3]);
				return config;
			}
		}
		return null;
	}
	
	public boolean setInvOutPutData(Reader reader, byte antenna, byte rssi,byte deviceNo, byte accessDoorDirection) {
		if (reader == null) {
			return false;
		}
		byte buf[] = new byte[16];
		buf[0] = antenna;
		buf[1] = rssi;
		buf[2] = deviceNo;
		buf[3] = accessDoorDirection;
		byte _buf = BitOperation.bytesToByte(buf);
		buf[0] = _buf;
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_SET_INV_OUTPUT_DATA, buf, 1)) {
			if (readData(reader, CMD.UHF_SET_INV_OUTPUT_DATA, buffer, 1)) {
				return true;
			}
		}
		return false;
	}

	public Map<String,Byte> getAntState(Reader reader) {
		if (reader == null) {
			return null;
		}
		Map<String, Byte> config = null;
		byte buf[] = new byte[16];
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_GET_ANT_STATE, buf, 1)) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (readData(reader, CMD.UHF_GET_ANT_STATE, buffer, 1)) {
				byte [] _buf = buffer.array();
				int lengthChannel = DataConvert.byteToInt(_buf[0]);
				if(lengthChannel < 4){
					return null;
				}
				config = new HashMap<String,Byte>();
				config.put("Channel", _buf[0]);
				for (int j = 0; j < lengthChannel; j++) {
					int index = j + 1;
					config.put("Ant" + index, _buf[index]);
				}
				return config;
			}
		}
		return null;
	}

	public boolean factoryDataReset(Reader reader) {
		if (reader == null) {
			return false;
		}
		byte buf[] = new byte[16];
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_FACTORY_DATA_RESET, buf, 1)) {
			if (readData(reader, CMD.UHF_FACTORY_DATA_RESET, buffer, 1)) {
				return true;
			}
		}
		return false;
	}

	private List<Boolean> frequencyPoint(byte [] result){
		List<Boolean> total = new ArrayList<Boolean>();
		int index = 0;
		for (int i = 0; i < 7; i++) {
			boolean [] point = BitOperation.byteToBooleans(result[i + 1]);
			for (int j = point.length - 1; j >= 0; j--) {
				if(index == 50){
					break;
				}
				total.add(point[j]);
				index++;
			}
		}
		return total;
	}
	
	private double frequencyFixed(byte [] result){
		int [] frequency = new int[3];
		for (int i = 0; i < frequency.length; i++) {
			frequency[i] = DataConvert.byteToInt(result[i+1]);
		}
		double frequencyFixed = frequency[0] * 256 * 256 + frequency[1] * 256 + frequency[2];
		return frequencyFixed/1000;
	}
		
	private byte[] frequencyFixed(int result){
		byte [] frequency = new byte[3];
		frequency[0] = (byte) (result >> 16);
		frequency[1] = (byte) ((result >> 8) % 256);
		frequency[2] = (byte) (result % 256);
		return frequency;
	}
	
	public FrequencyPoint getFrequency(Reader reader) {
		if (null == reader) {
			return null;
		}
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_GET_FREQUENCY, null, 0)) {
			if (readData(reader, CMD.UHF_GET_FREQUENCY, buffer, 0)) {
				FrequencyPoint frequencyPoint = new FrequencyPoint();
				int length = 8;
				byte [] result = Arrays.copyOf(buffer.array(),length);
				int type = DataConvert.byteToInt(result[0]);
				if(type == 5){
					frequencyPoint.setFrequencyHopping(frequencyPoint(result));
				}else if(type == 6){
					frequencyPoint.setFrequencyFixed(frequencyFixed(result));
				}
				frequencyPoint.setType(type);
				return frequencyPoint;
			}
		}
		return null;
	}

	private byte [] frequencyHoppingFilter(boolean[] frequencyHopping){
		boolean [] frequency = new boolean[50];
		for (int i = 0; i < frequencyHopping.length; i++) {
			frequency[i] = frequencyHopping[i];
		}
		byte[] value = new byte[7];
		value[0] = BitOperation.booleansReversalToByte(frequency, 0, 8);
		value[1] = BitOperation.booleansReversalToByte(frequency, 8, 16);
		value[2] = BitOperation.booleansReversalToByte(frequency, 16, 24);
		value[3] = BitOperation.booleansReversalToByte(frequency, 24, 32);
		value[4] = BitOperation.booleansReversalToByte(frequency, 32, 40);
		value[5] = BitOperation.booleansReversalToByte(frequency, 40, 48);
		value[6] = BitOperation.booleansReversalToByte(frequency, 48, 50);
		return value;
	}
		
	public boolean setFrequency(Reader reader,int type,double frequencyFixed,boolean[] frequencyHopping) {
		if (null == reader) {
			return false;
		}
		if(type < 0 || type > 6){
			return false;
		}
		if(null == frequencyHopping || frequencyHopping.length > 50){
			return false;
		}
		byte buf[] = new byte[16];
		buf[0] = (byte) type;
		if(type == 5){
			byte [] hopping = frequencyHoppingFilter(frequencyHopping);
			System.arraycopy(hopping, 0, buf, 1, 7);
		}else if(type == 6){
			if(frequencyFixed > 1000){
				return false;
			}
			int value = (int) (frequencyFixed * 1000);
			byte [] fixed = frequencyFixed(value);
			System.arraycopy(fixed, 0, buf, 1, 3);
		}
		
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_SET_FREQUENCY, buf, 8)) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (readData(reader, CMD.UHF_SET_FREQUENCY, buffer, 0)) {
				if (data[0] != ERROR.HOST_ERROR) {
					return true;
				}
			}
		}
		return false;
	}
	
	/*****************************reserved 2018-12-07 start***************************************/
	/**
	 * ����Digital Output״̬
	 */
	boolean setDO(Reader reader, int port, int state) {
		if (null == reader) {
			return false;
		}
		byte buf[] = new byte[32];
		if (port > 2 || port == 0) {
			return false;
		}
		buf[0] = (byte) port;
		buf[1] = (byte) state;
		ByteBuffer buffer = ByteBuffer.allocate(10);
		if (sendData(reader, CMD.UHF_SET_DO_STATE, buf, 2)) {
			if (readData(reader, CMD.UHF_SET_DO_STATE, buffer, 1)) {
				if (data[0] != ERROR.HOST_ERROR) {
					return true;
				}
			}
		}
		return false;
	}
	
	boolean getClock(Reader reader, ByteBuffer clock) {
		if (null == reader) {
			return false;
		}
		ByteBuffer buffer = ByteBuffer.allocate(10);
		if (sendData(reader, CMD.UHF_GET_CLOCK, null, 0)) {
			if (readData(reader, CMD.UHF_GET_CLOCK, buffer, 6)) {
				if (data[0] != ERROR.HOST_ERROR) {
					clock.put(Arrays.copyOf(buffer.array(), 6));
					return true;
				}
			}
		}
		return false;
	}
	
	boolean setClock(Reader reader, byte[] clock) {
		if (null == reader) {
			return false;
		}
		byte buf[] = new byte[16];
		System.arraycopy(clock, 0, buf, 0, 6);
		ByteBuffer buffer = ByteBuffer.allocate(10);
		if (sendData(reader, CMD.UHF_SET_CLOCK, buf, 6)) {
			if (readData(reader, CMD.UHF_SET_CLOCK, buffer, 1)) {
				if (data[0] != ERROR.HOST_ERROR) {
					return true;
				}
			}
		}
		return false;
	}

	boolean setReadZone(Reader reader, byte state) {
		if (null == reader) {
			return false;
		}
		byte buf[] = new byte[16];
		ByteBuffer buffer = ByteBuffer.allocate(16);
		if (0 == state) {
			buf[0] = 0;
		} else {
			buf[0] = 1;
		}
		if (sendData(reader, CMD.UHF_SET_READ_ZONE, buf, 1)) {
			if (readData(reader, CMD.UHF_SET_READ_ZONE, buffer, 1)) {
				return true;
			}
		}
		return false;
	}

	boolean getReadZone(Reader reader, ByteBuffer zone) {
		if (null == reader) {
			return false;
		}
		ByteBuffer buffer = ByteBuffer.allocate(16);
		if (sendData(reader, CMD.UHF_GET_READ_ZONE, null, 0)) {
			if (readData(reader, CMD.UHF_GET_READ_ZONE, buffer, 1)) {
				if (data[0] != ERROR.HOST_ERROR) {
					zone.put(buffer.array()[0]);
					return true;
				}
			}
		}
		return false;
	}

	boolean getReadZonePara(Reader reader, ByteBuffer bank, ByteBuffer begin,
			ByteBuffer length) {
		if (null == reader) {
			return false;
		}
		ByteBuffer buffer = ByteBuffer.allocate(16);
		if (sendData(reader, CMD.UHF_GET_READZONE_PARA, null, 0)) {
			if (readData(reader, CMD.UHF_GET_READZONE_PARA, buffer, 3)) {
				if (data[0] != ERROR.HOST_ERROR) {
					bank.put(buffer.array()[0]);
					begin.put(buffer.array()[1]);
					length.put(buffer.array()[2]);
					return true;
				}
			}
		}
		return false;
	}
	
	boolean setReadZonePara(Reader reader, byte bank, byte begin, byte length) {
		if (null == reader) {
			return false;
		}
		byte buf[] = new byte[16];
		buf[0] = bank;
		buf[1] = begin;
		buf[2] = length;
		ByteBuffer buffer = ByteBuffer.allocate(16);
		if (sendData(reader, CMD.UHF_SET_READZONE_PARA, buf, 3)) {
			if (readData(reader, CMD.UHF_SET_READZONE_PARA, buffer, 1)) {
				return true;
			}
		}
		return false;
	}
	
	boolean readTagBuffer(Reader reader, CallBack getReadData, int readTime) {
		if (null == reader) {
			return false;
		}
		if (sendData(reader, CMD.UHF_GET_TAG_BUFFER, null, 0)) {
			// ��δ��
			return true;
		}
		return false;
	}
	
	boolean resetTagBuffer(Reader reader) {
		if (null == reader) {
			return false;
		}
		ByteBuffer buffer = ByteBuffer.allocate(16);
		if (sendData(reader, CMD.UHF_RESET_TAG_BUFFER, null, 0)) {
			if (readData(reader, CMD.UHF_RESET_TAG_BUFFER, buffer, 1)) {
				return true;
			}
		}
		return false;
	}
	
	boolean setAlive(Reader reader, byte interval) {
		if (reader == null) {
			return false;
		}
		byte buf[] = new byte[16];
		buf[0] = interval;
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_ALIVE, buf, 1)) {
			if (readData(reader, CMD.UHF_ALIVE, buffer, 1)) {
				return true;
			}
		}
		return false;
	}
	
	boolean getDeviceConfig(Reader reader, ByteBuffer para) {
		if (reader == null) {
			return false;
		}
		ByteBuffer buffer = ByteBuffer.allocate(100);
		if (sendData(reader, CMD.UHF_GET_CONFIGURE, null, 0)) {
			if (readData(reader, CMD.UHF_GET_CONFIGURE, buffer, 20)) {
				return true;
			}
		}
		return false;
	}
	/*****************************reserved 2018-12-07 end****************************************/
}



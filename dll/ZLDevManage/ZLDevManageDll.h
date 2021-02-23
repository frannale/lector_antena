/* ���ļ������˵���ZlDevManageDll�ĺ궨��ͺ��� */
#ifndef __INCLUDE_ZL_TYPES_H__


#endif

/* �궨��
*/
#define ZLDM_VER						"V1.41"			// ��汾��
#define ID_LEN							6				// hex�͵��豸id�ĳ���
#define ID_STRING_LEN					(ID_LEN*2)		// �ַ����͵��豸id�ĳ���
#define ZLDM_HANDLER_ARRAY_MIN_SIZE		256				// ������豸���������Ա�256����

/* ���º궨�嶼�ǻ�á����ò���ʱ�Ĳ������Ͷ��壬��ϸ�ο����������ĵ�����1
*/

#define PARAM_SET_TO_DEFAULT			0			

#define PARAM_DEV_EXIST_IN_SUBNET		1					
#define PARAM_DEV_LOCAL_IP				2			
#define PARAM_DEV_LOCAL_PORT			3			
#define PARAM_DEV_OUTER_IP              		4			
#define PARAM_ALL_SUBNET_ID             		5			
#define PARAM_SIMULATE_PORT             		6			
#define PARAM_P2P_STATUS_CODE			7			
#define PARAM_P2P_STATUS_CHS			8			
#define PARAM_P2P_STATUS_EN			9		

#define PARAM_WORK_MODE				256			
#define PARAM_IP_MODE					257
#define PARAM_FLOW_CONTROL			258
#define PARAM_DEST_MODE				259	
#define PARAM_APP_PROTOCOL				260
#define PARAM_STOP_BIT					261
#define PARAM_PARITY					262
#define PARAM_DATA_BITS					263
#define PARAM_BAUNDRATE					264
#define PARAM_DNS_SERVER_IP				265
#define PARAM_UDP_GROUP_IP				266
//#define PARAM_LOCAL_IP					267	
#define PARAM_NET_MASK					268
#define PARAM_GATEWAY					269
#define PARAM_RECONNECT_TIME			270
#define PARAM_KEEP_ALIVE_TIME			271
#define PARAM_WEB_PORT					272
#define PARAM_DEV_VER					273
//#define PARAM_LOCAL_PORT				274
#define PARAM_DEST_PORT					275
#define PARAM_DEV_NAME					276
#define PARAM_DEST_IP					277
#define PARAM_GAP_TIME					278
#define PARAM_PACKING_LEN				279
#define PARAM_LINK_STATUS				280
#define PARAM_RESOURCE_IP				281
#define PARAM_RESOURCE_PORT			282

#ifndef P2P_GET_STATE_UNKOWN
#define P2P_GET_STATE_UNKOWN		0		// δ֪
#define P2P_GET_STATE_UNCONECTED	1		// �ͷ�����δ����
#define P2P_GET_STATE_PORT_ERR		2		// ����Ķ˿ڲ�����
#define P2P_GET_STATE_SUBNET		3		// ����
#define P2P_GET_STATE_INTERNET		4		// ����
#define P2P_GET_STATE_PROXY			5		// ����
#define P2P_GET_STATE_CONECTTING	6		// �����л��ߵȴ�����
#define P2P_GET_STATE_WAIT_CON		7		// �ȴ�p2p���ӵ���
#define P2P_GET_STATE_SERVER		8		// �Ƿ�����ģʽ������Ϊpcͨ�������������֪���Ƿ������p2popen����
#define P2P_GET_STATE_SERVER_AUTH_ERR	9	// pc���߷�����δ��֤�������ĺϷ���
#define P2P_GET_STATE_DEV_OFF_LINE	10		// pc���ӵ�dev��������״̬
#define P2P_GET_STATE_DEV_AUTH_ERR	11		// pc���ӵ�devδ����������֤ͨ��
#define P2P_GET_STATE_PC_AUTH_ERR	12		// pcδ����������֤ͨ��
#define P2P_GET_STATE_DEV_BELONG_ERR	13	// pcϣ�����ӵ�dev����������û�����
#define P2P_GET_STATE_SERVER_NO_ACK	14	// ���������������������û��Ӧ��
#define P2P_GET_STATE_DEV_NO_ACK		15	// ����������������Ӧ���豸û��Ӧ��
#define P2P_GET_STATE_WAIT_PROXY		16	// ����ͨ������ʽ����
#endif

/* ����ָ�붨�� */
typedef int	(__stdcall * tZLDM_Init)(int ServerPort);
typedef void	(__stdcall * tZLDM_Exit)();
typedef char *	(__stdcall * tZLDM_GetVer)();

typedef int	(__stdcall * tZLDM_StartSearchDev)();
typedef char * (__stdcall * tZLDM_GetDevID)(int Dev_Index);

typedef int (__stdcall * tZLDM_AddManualDev)(const char ip_dns1[], const char ip_dns2[], int port);
typedef void	(__stdcall * tZLDM_ClearManualDev)();

typedef  char * (__stdcall *tZLDM_GetDevParamString)(const char *id, int ParamType);
typedef  int (__stdcall *tZLDM_GetDevParamInt)(const char *id, int ParamType);
typedef  int (__stdcall *tZLDM_SetDevParamString)(const char *id, const char *NewParam, int ParamType);
typedef  int (__stdcall *tZLDM_SetDevParamInt)(const char *id, int NewParam, int ParamType);
typedef  int (__stdcall *tZLDM_SetDevParamExcute)(const char *id);


/* p2p functions */
typedef  bool (__stdcall *tZLDM_P2pOpen)(const char* svraddr,int svrport, const char* username ,const char* userkey, int retry_time);
typedef  bool (__stdcall *tZLDM_P2pClose)();
typedef  int (__stdcall *tZLDM_P2pAddPort)(int simulate_port, const char* dev_id, const char *connect_to_ip, unsigned short connect_to_port);
typedef  bool (__stdcall *tZLDM_P2pDelPort)(int simulate_port);
typedef  void (__stdcall *tZLDM_P2pRestartAll)();
typedef  bool (__stdcall *tZLDM_P2pRetartPort)(int simulate_port);
typedef  char * (__stdcall *tZLDM_P2pGetID)(int ithP2pDev);




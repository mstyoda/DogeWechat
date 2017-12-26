#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <string.h>
#include <unistd.h>
#include <netdb.h>
#include <sys/socket.h>
#include <sys/fcntl.h>
#include <netinet/in.h>
#include <sys/types.h>
#include <arpa/inet.h>
#include <pthread.h>
#include <fstream>
#include <cerrno>

#define PORT 15636
#define PACKAGE_SIZE 256
#define N_USER 3

#define rep(i,l,r) for (int i=l;i<=r;i++)
#define drep(i,l,r) for (int i=l;i>=r;i--)
using namespace std;


void *dealRequest(void *vargp); 

int listenfd;

int tfiles = 0;

int isFriend[10][10];

struct sockaddr_in serverAddr;

struct User
{
    char name[20];
    char password[20];
    int userId;
    int socketId;

    User(){}
    User(char Name[20], char Password[20], int UserId)
    {
        strcpy(name,Name);
        strcpy(password,Password);
        userId = UserId;
        socketId = -1;
    }
}users[10];

struct Afile
{
    char srcName[25];
    char dstName[25];
    char fileName[105];

    Afile(){}
    Afile(char * _srcName, char* _dstName, char* _fileName)
    {
        strcat(srcName,_srcName); 
        strcat(dstName,_dstName); 
        strcat(fileName,_fileName);
    }
}files[100];

void initServer() 
{
    listenfd = socket(AF_INET, SOCK_STREAM,0);
    if (listenfd < 0) //Error
    {
        perror("socket"); exit(1);
    }

    bzero((char *)&serverAddr,sizeof(serverAddr));  
    serverAddr.sin_family = AF_INET;  
    serverAddr.sin_addr.s_addr = htonl(INADDR_ANY);  
    serverAddr.sin_port = htons(PORT);

    if(bind(listenfd,(struct sockaddr *)&serverAddr,sizeof(serverAddr)) < 0)
    {  
            perror("connect");  
            exit(1);  
    }  
    
    printf("Create Server Successfully!\n");
    
    if(listen(listenfd,1024) < 0)
    {  
            perror("listen error");  
            exit(1);  
    }

}

void initUser()
{

    //3 User
    users[0] = User((char *)("Alice\0"),(char *)("AliceAlice\0"),0);
    users[1] = User((char *)("Bob\0"),(char *)("BobBob\0"),1);
    users[2] = User((char *)("yoda\0"),(char *)("yoda\0"),2);

    rep(i,0,2){ rep(j,0,2) isFriend[i][j] = 0; isFriend[i][i] = 1;}
    //printf("%s\n",users[2].name);
}

void working()
{

    struct sockaddr_in clientAddr;
    int clientLen, *connfdp;

    while (1)
    {
        connfdp = (int *)malloc(sizeof(int));
        *connfdp = accept(listenfd,(struct sockaddr * )&clientAddr,(unsigned int * )&clientLen);
        //get socket for this request
        pthread_t tid;
        pthread_create(&tid,NULL,dealRequest,connfdp);
        printf("connected !\n");
        //create a new thread to deal this request;
    }
   //Wait for connect or request 
}

void getSubString(char *dst, char *src, int l,int r)
{
    int k = 0;
    rep(i,l,r) dst[k++] = src[i];
    //rep(i,l,r) dst[k++] = '0'+k;
    dst[k++] = '\0';
}

int cmpStr(char *a, char *b)
{
    if (strlen(a) != strlen(b)) return 0;
    rep(i,0,(int)strlen(a) - 1) if (a[i] != b[i]) return 0;
    return 1;
}
int getUser(char *name, char *password)
{
    rep(i,0,N_USER - 1)
    {
        printf("%s %d %s %d %d\n",name,(int)strlen(name),users[i].name,(int)strlen(users[i].name),cmpStr(name, users[i].name));
        if (cmpStr(name, users[i].name) && cmpStr(password, users[i].password))
        {
            return users[i].userId;   
        }
    }
    return -1;
}

int getUserSocket(char *name)
{
    rep(i,0,N_USER - 1) if (cmpStr(name, users[i].name))
    {
        return users[i].socketId;
    }
    return -1;
}

void getResponseForLogin(char *name, char *password, char *response)
{
    //command[1] = 'l'
    //state[1] = 'y' or 'n'
    
    response[0] = 'l';
    int userId = getUser(name,password);

    if (userId == -1)
        response[1] = 'n';
    else 
    {
        response[1] = 'y';
        int cnt = 0;
        rep(i,0,N_USER - 1) if (isFriend[userId][i])
        {   
            int len = strlen(users[i].name);
            rep(j,0,len - 1) response[j + 3 + cnt * 20] =  users[i].name[j];
            response[len + 3 + cnt * 20] = '\0';
            cnt++;
        }
        response[2] = cnt + '0';
    }
}

void makeFriends(char *aName, char* bName) {
    int a,b;
    rep(i,0,N_USER - 1) if (cmpStr(aName,users[i].name)) a = i;
    rep(i,0,N_USER - 1) if (cmpStr(bName,users[i].name)) b = i;
    isFriend[a][b] = isFriend[b][a] = 1;
}


void getResponseForNewFriends(char *name, char * response) {
    response[0] = 'n';
    response[1] = 'y';
    int cnt = 0,userId = -1;
    rep(i,0,N_USER - 1) if (cmpStr(users[i].name,name)) userId = i;
    rep(i,0,N_USER - 1) if (isFriend[userId][i])
    {   
        int len = strlen(users[i].name);
        rep(j,0,len - 1) response[j + 3 + cnt * 20] =  users[i].name[j];
        response[len + 3 + cnt * 20] = '\0';
        cnt++;
    }
    response[2] = cnt + '0';
}
void getResponseForSearch(char * dstName, char *response)
{
    //command[1] = 'a'
    //result[1] = 'y' or 'n'
    //dstname[20]

    printf("fuck response for search!\n");
    response[0] = 'a';
    int userId = -1;
    rep(i,0,N_USER - 1) if (cmpStr(users[i].name,dstName)) userId = i;
    if (userId != -1) {
        response[1] = 'y';
        int len = strlen(dstName);
        rep(j,0,len - 1) response[2 + j] = dstName[j];
        response[len + 2] = '\0';
    }
    printf("pack = %s\n",response);
}

void getFilePath(char * dstName, char * fileName, char * path) {
    strcpy(path,"downloads/");
    int cnt = strlen(path);
    rep(i,0,strlen(dstName) - 1) path[cnt++] = dstName[i];
    path[cnt++] = '/';
    rep(i,0,strlen(fileName) - 1) path[cnt++] = fileName[i];
    path[cnt++] = '\0';
    printf("path = %s\n",path);
}
void *dealRequest(void *vargp)
{
    int connfd = *((int *) vargp);
    int state = 1;
    int thisUserId = -1;

    char pack[PACKAGE_SIZE];
    while (1)
    {
        state = recv(connfd,pack,PACKAGE_SIZE,0);
        if (state == 0) 
        {
            if (thisUserId != -1) 
                users[thisUserId].socketId = -1;
            close(connfd);
            break;

        }
        printf("receive a pack!\n");
        char commad;
        commad = pack[0];
        printf("commad = %c\n",commad);
        //get Pack from socket
        if (commad == 'l')
        //login (commad[1],name[20],password[20],padding[...])
        {
            char name[25]; char password[25];
            getSubString(name,pack,1,20);
            getSubString(password,pack,21,40);
            //printf("Username = %s\n",name);
            //printf("Password = %s\n",password);
            //get username and password

            char response[PACKAGE_SIZE];
            getResponseForLogin(name, password, response);

            thisUserId = getUser(name,password);
            users[thisUserId].socketId = connfd;
                
            send(connfd,response,PACKAGE_SIZE,0);

        } 

        else if (commad == 'q')
        //quit connect (command[1],...)
        {
            send(connfd,pack,PACKAGE_SIZE,0);
            close(connfd); break;
        } 

        else if (commad == 's')
        //send message (command[1],srcName[20],dstName[20]...)
        {
            char dstName[25];
            getSubString(dstName,pack,21,40);
            int dstfd = getUserSocket(dstName);
            send(dstfd,pack,PACKAGE_SIZE,0);
        }
        else if (commad == 'f')
        {
            char srcName[25];
            char dstName[25];
            char fileName[105];
            char path[105];
            getSubString(srcName,pack,1,20);
            getSubString(dstName,pack,21,40);
            getSubString(fileName,pack,41,140);

            char response[PACKAGE_SIZE];
            response[0] = '0';//ack
            send(connfd,response,PACKAGE_SIZE,0);
            int targetFile;
            getFilePath(dstName,fileName,path);
            int target = open(path,O_WRONLY|O_CREAT|O_TRUNC,0644);
            files[++tfiles] = Afile(srcName,dstName,fileName);
            
            char buf[512]; memset(buf,0,512);
            int bytes;
            
            recv(connfd,buf,512,0);//magic option,don't touch!
            while ((bytes = recv(connfd,buf,512,0)) > 0)
            {
                write(target,buf,bytes);
                memset(buf,0,512);
                send(connfd,response,PACKAGE_SIZE,0);
                printf("receive a pack of file :%d\n",bytes);
                if (bytes < 512) break; else printf("fucker");
            }
            printf("finished!");
            close(target);
        }
        else if (commad == 'a') {// search friends
            char srcName[25];
            char dstName[25];
            getSubString(srcName,pack,1,20);
            getSubString(dstName,pack,21,40);
            char response[PACKAGE_SIZE];
            getResponseForSearch(dstName, response);
            send(connfd,response,PACKAGE_SIZE,0);
        }
        else if (commad == 'n') { //new friends
            char srcName[25];
            char dstName[25];
            getSubString(srcName,pack,1,20);
            getSubString(dstName,pack,21,40);
            makeFriends(srcName,dstName);
            rep(i,0,N_USER - 1) if (users[i].socketId != -1) {
                char response[PACKAGE_SIZE];
                getResponseForNewFriends(users[i].name,response);
                send(users[i].socketId,response,PACKAGE_SIZE,0);
            }
        }
    }
}

void forfun()
{
    int target,src;
    std :: fstream fin;
    fin.open("wonder.jpeg",ios_base::in | ios_base::binary);

    target = open("b.jpeg",O_WRONLY|O_CREAT|O_TRUNC,0644);
    bool next = true;
    char buffer[256];
    while (next)
    {
        int i = 0;
        char c;
        for (i = 0; i < 256 && fin.read(&c,1);i++)
            buffer[i] = c;
        if (i < 256) next = false;
        write(target,buffer,i);
    }
}

int main()
{
    //forfun();
    
    initUser();
    initServer();
    working();
}
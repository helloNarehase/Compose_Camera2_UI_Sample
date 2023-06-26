class MainActivity : ComponentActivity() {
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private lateinit var previewRequest: CaptureRequest
    private lateinit var textureView:TextureView
    private var imageReader: ImageReader? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CameraView(modifier = Modifier
                            .size(300.dp,400.dp)
                            .clip(shape = RoundedCornerShape(50.dp))
                            .background(Color.Cyan))
                    }
                }
            }
        }
    }
    @Composable
    fun CameraView(modifier: Modifier) {
        val cn = LocalContext.current
        val cameraManager = cn.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        fun createCameraPreviewSession() {
            val cameraId = cameraManager.cameraIdList[0] // 첫 번째 카메라로 가정
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val outputSizes = streamConfigurationMap?.getOutputSizes(ImageFormat.JPEG)
            val previewSize = outputSizes?.get(0) ?: Size(640, 480)
            val surfaceTexture = textureView.surfaceTexture


            // 원하는 크기로 이미지 크기 설정 (여기서는 첫 번째 크기로 가정)

            surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
            val surface = android.view.Surface(surfaceTexture)

            imageReader = ImageReader.newInstance(
                previewSize.width, previewSize.height, ImageFormat.JPEG, 2
            )
            imageReader?.setOnImageAvailableListener({ reader ->
                // 이미지가 사용 가능한 경우 호출되는 콜백
                val image = reader.acquireLatestImage()
                // 이미지 처리 로직을 구현하세요
                // 예를 들어, 이미지를 저장하거나 표시할 수 있습니다.
                image?.close()
            }, null)

            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder.addTarget(surface)
            previewRequestBuilder.addTarget(imageReader!!.surface)

            cameraDevice.createCaptureSession(
                listOf(surface, imageReader?.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        previewRequestBuilder.set(
                            CaptureRequest.CONTROL_MODE,
                            CameraMetadata.CONTROL_MODE_AUTO
                        )
                        previewRequest = previewRequestBuilder.build()
                        captureSession.setRepeatingRequest(previewRequest, null, null)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        // 캡처 세션 구성 실패 처리
                    }
                },
                null
            )
        }

        AndroidView(factory = {   context ->
            textureView = CameraViews(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            textureView.surfaceTextureListener = object : SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                    val cameraId = cameraManager.cameraIdList[0] // 사용 가능한 첫 번째 카메라 ID 선택

                    if (ActivityCompat.checkSelfPermission(cn, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(cn as Activity, arrayOf(Manifest.permission.CAMERA), 0)
                        return
                    }

                    cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                        override fun onOpened(camera: CameraDevice) {
                            cameraDevice = camera
                            createCameraPreviewSession()
                        }

                        override fun onDisconnected(camera: CameraDevice) {
                            cameraDevice.close()
                        }

                        override fun onError(camera: CameraDevice, error: Int) {
                            cameraDevice.close()
                        }
                    }, null)
                }

                override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                    // SurfaceTexture의 크기가 변경된 경우 호출되는 콜백
                    // 적절한 크기로 SurfaceTexture가 변경되면 captureSession을 다시 설정할 수 있습니다.
                }

                override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                    return true
                }

                override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
                    // SurfaceTexture의 업데이트가 발생한 경우 호출되는 콜백
                    // 프레임마다 추가 처리를 할 수 있습니다.
                }


            }

            textureView
        },
            modifier = modifier
        )
    }
}

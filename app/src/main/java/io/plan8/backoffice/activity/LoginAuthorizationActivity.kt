package io.plan8.backoffice.activity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import retrofit2.Callback
import io.plan8.backoffice.BR
import io.plan8.backoffice.Constants
import io.plan8.backoffice.R
import io.plan8.backoffice.SharedPreferenceManager
import io.plan8.backoffice.adapter.RestfulAdapter
import io.plan8.backoffice.databinding.ActivityLoginAuthorizationBinding
import io.plan8.backoffice.model.api.AuthInfo
import io.plan8.backoffice.model.api.Me
import io.plan8.backoffice.model.api.Team
import io.plan8.backoffice.util.ViewUtil
import io.plan8.backoffice.vm.LoginAuthorizationActivityVM
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.util.*

class LoginAuthorizationActivity : BaseActivity(), TextView.OnEditorActionListener, View.OnClickListener {
    private lateinit var binding: ActivityLoginAuthorizationBinding
    private var vm: LoginAuthorizationActivityVM? = null
    private lateinit var authoTitle: TextView
    private var progressBar: RelativeLayout? = null
    private var userPhoneNumber: String? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var firstInput: TextView? = null
    private var secondInput: TextView? = null
    private var thirdInput: TextView? = null
    private var fourthInput: TextView? = null
    private var fifthInput: TextView? = null
    private var sixthInput: TextView? = null
    private var inputField: LinearLayout? = null
    private var authoEditText: EditText? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login_authorization)
        vm = LoginAuthorizationActivityVM(this, savedInstanceState)
        binding.setVariable(BR.vm, vm)
        binding.executePendingBindings()

        val phoneNumber = intent.getStringExtra("phoneNumber")

        authoTitle = binding.authorizationTitle
        authoTitle.text = "'$phoneNumber’ 번호로\n인증번호 문자메시지가 발송되었습니다.\n4자리 인증번호를 입력해주세요."
        authoEditText = binding.authorizationCodeInputEditText
        authoEditText!!.setOnEditorActionListener(this)

        progressBar = binding.loginAuthProgressBarContainer

        firstInput = binding.firstInput
        secondInput = binding.secondInput
        thirdInput = binding.thirdInput
        fourthInput = binding.fourthInput
        fifthInput = binding.fifthInput
        sixthInput = binding.sixthInput
        inputField = binding.authoInputField
        inputField!!.setOnClickListener(this)
        binding.authNextStep.setOnClickListener { if (authoEditText!!.text.length >= 6) nextStep() }
        binding.authPrevStep.setOnClickListener { onBackPressed() }

        val focusLineList = ArrayList<View>()
        focusLineList.add(binding.firstLine)
        focusLineList.add(binding.secondLine)
        focusLineList.add(binding.thirdLine)
        focusLineList.add(binding.fourthLine)
        focusLineList.add(binding.fifthLine)
        focusLineList.add(binding.sixthLine)

        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(edit: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.length > 0) {
                    val position = start - before
                    if (position == -1) {
                        firstInput!!.text = ""
                        for (i in focusLineList.indices) {
                            if (i == position + 1) {
                                focusLineList[position + 1].visibility = View.VISIBLE
                            } else {
                                focusLineList[i].visibility = View.GONE
                            }
                        }
                    } else if (position == 0) {
                        if (count == 4) {
                            firstInput!!.text = s[0] + ""
                            secondInput!!.text = s[1] + ""
                            thirdInput!!.text = s[2] + ""
                            fourthInput!!.text = s[3] + ""
                            for (i in focusLineList.indices) {
                                focusLineList[i].visibility = View.GONE
                            }
                        } else {
                            firstInput!!.text = s[position] + ""
                            for (i in focusLineList.indices) {
                                if (i == position + 1) {
                                    focusLineList[position + 1].visibility = View.VISIBLE
                                } else {
                                    focusLineList[i].visibility = View.GONE
                                }
                            }
                            secondInput!!.text = ""
                        }
                    } else if (position == 1) {
                        secondInput!!.text = s[position] + ""
                        for (i in focusLineList.indices) {
                            if (i == position + 1) {
                                focusLineList[position + 1].visibility = View.VISIBLE
                            } else {
                                focusLineList[i].visibility = View.GONE
                            }
                        }
                        thirdInput!!.text = ""
                    } else if (position == 2) {
                        thirdInput!!.text = s[position] + ""
                        for (i in focusLineList.indices) {
                            if (i == position + 1) {
                                focusLineList[position + 1].visibility = View.VISIBLE
                            } else {
                                focusLineList[i].visibility = View.GONE
                            }
                        }
                        fourthInput!!.text = ""
                    } else if (position == 3) {
                        fourthInput!!.text = s[position] + ""
                        for (i in focusLineList.indices) {
                            if (i == position + 1) {
                                focusLineList[position + 1].visibility = View.VISIBLE
                            } else {
                                focusLineList[i].visibility = View.GONE
                            }
                        }
                        fifthInput!!.text = ""
                    } else if (position == 4) {
                        fifthInput!!.text = s[position] + ""
                        for (i in focusLineList.indices) {
                            if (i == position + 1) {
                                focusLineList[position + 1].visibility = View.VISIBLE
                            } else {
                                focusLineList[i].visibility = View.GONE
                            }
                        }
                        sixthInput!!.text = ""
                    } else if (position == 5) {
                        sixthInput!!.text = s[position] + ""
                        for (i in focusLineList.indices) {
                            focusLineList[i].visibility = View.GONE
                        }
                        if (s.length == 6) {
//                            requestOAuth(s.toString())
                            nextStep()
                        }
                    }
                } else {
                    for (i in focusLineList.indices) {
                        if (i == 0) {
                            focusLineList[i].visibility = View.VISIBLE
                        } else {
                            focusLineList[i].visibility = View.GONE
                        }
                    }
                    firstInput!!.text = ""
                    secondInput!!.text = ""
                    thirdInput!!.text = ""
                    fourthInput!!.text = ""
                    fifthInput!!.text = ""
                    sixthInput!!.text = ""
                }
            }
        }

        authoEditText!!.addTextChangedListener(textWatcher)

        authoEditText!!.isFocusableInTouchMode = true
        authoEditText!!.requestFocus()
        ViewUtil.showKeyboard(authoEditText!!)
    }

    //TODO : 문자인증번호 파싱 리시버 로직임. 필요할때 주석제거
//    private fun registerSMSReceiver() {
//        val action = "android.provider.Telephony.SMS_RECEIVED"
//        val logTag = "SmsReceiver"
//
//        val intentFilter = IntentFilter()
//        intentFilter.addAction(action)
//        broadcastReceiver = object : BroadcastReceiver() {
//            override fun onReceive(context: Context, intent: Intent) {
//                if (intent.action == action) {
//                    //Bundel 널 체크
//                    val bundle = intent.extras ?: return
//
//                    //pdu 객체 널 체크
//                    val pdusObj = bundle.get("pdus") as Array<Any> ?: return
//
//                    //message 처리
//                    val smsMessages = arrayOfNulls<SmsMessage>(pdusObj.size)
//                    for (i in pdusObj.indices) {
//                        smsMessages[i] = SmsMessage.createFromPdu(pdusObj[i] as ByteArray)
//                        Log.e(logTag, "NEW SMS " + i + "th")
//                        Log.e(logTag, "DisplayOriginatingAddress : " + smsMessages[i].getDisplayOriginatingAddress())
//                        Log.e(logTag, "DisplayMessageBody : " + smsMessages[i].getDisplayMessageBody())
//                        Log.e(logTag, "EmailBody : " + smsMessages[i].getEmailBody())
//                        Log.e(logTag, "EmailFrom : " + smsMessages[i].getEmailFrom())
//                        Log.e(logTag, "OriginatingAddress : " + smsMessages[i].getOriginatingAddress())
//                        Log.e(logTag, "MessageBody : " + smsMessages[i].getMessageBody())
//                        Log.e(logTag, "ServiceCenterAddress : " + smsMessages[i].getServiceCenterAddress())
//                        Log.e(logTag, "TimestampMillis : " + smsMessages[i].getTimestampMillis())
//
//                        val noSpaceStr = smsMessages[i].getMessageBody()
//                        noSpaceStr.replace("\\p{Z}", "")
//                        val authoNumber = smsMessages[i].getMessageBody().split("인증번호:".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].split("\\(3분간 유효합니다\\)".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
//
//                        if (authoNumber != null && (context.applicationContext as BaseApplication).getLoginAuthorizationActivity() != null) {
//                            progressBar!!.visibility = View.VISIBLE
//                            requestOAuth(authoNumber)
//                        }
//
//                        Log.e("message", smsMessages[i].getMessageBody())
//                    }
//                }
//            }
//        }
//
//        registerReceiver(broadcastReceiver, intentFilter)
//    }

    fun nextStep() {
        ViewUtil.hideKeyboard(authoEditText!!)
        progressBar!!.visibility = View.VISIBLE
        if (RestfulAdapter.instance!!.serviceApi != null) {
            RestfulAdapter.instance!!.serviceApi!!.getAuthInfo(intent.getStringExtra("code"), authoEditText!!.text.toString()).enqueue(object : Callback<AuthInfo> {
                override fun onFailure(call: Call<AuthInfo>?, t: Throwable?) {
                    Toast.makeText(applicationContext, "인증번호를 확인 해주세요.", Toast.LENGTH_SHORT).show()
                    progressBar!!.visibility = View.GONE
                    onBackPressed()
                }

                override fun onResponse(call: Call<AuthInfo>?, response: Response<AuthInfo>?) {
                    if (response?.body() != null) {
                        SharedPreferenceManager(applicationContext).userToken = response.body()!!.token
                        RestfulAdapter.instance!!.serviceApi!!.getMe("Bearer " + response.body()!!.token).enqueue(object : Callback<Me> {
                            override fun onResponse(call: Call<Me>?, response: Response<Me>?) {
                                if (response?.body() != null) {
                                    Constants.me = response.body()!!
                                    nextActivity()
                                }
                            }

                            override fun onFailure(call: Call<Me>?, t: Throwable?) {
                                Toast.makeText(applicationContext, "잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                            }

                        })
                    }
                }
            })
        }
    }

    private fun nextActivity() {
        progressBar!!.visibility = View.GONE
        startActivity(MainActivity.buildIntent(this))
        finish()
        overridePendingTransition(R.anim.pull_in_right_activity, R.anim.push_out_left_activity)
    }

    override fun onBackPressed() {
        val loginIntent = Intent(this, LoginActivity::class.java)
        startActivity(loginIntent)
        finish()
        overridePendingTransition(R.anim.pull_in_left_activity, R.anim.push_out_right_activity)
    }

    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean {
        if (v.id == authoEditText!!.id && actionId == EditorInfo.IME_ACTION_DONE) {
            nextStep()
        }
        return false
    }

//    override fun onDestroy() {
//        (applicationContext as BaseApplication).setLoginAuthorizationActivity(null)
//        super.onDestroy()
//    }

    override fun onClick(v: View) {
        if (authoEditText != null) {
            authoEditText!!.requestFocus()
            authoEditText!!.setSelection(authoEditText!!.text.length)
            authoEditText!!.isFocusableInTouchMode = true
            ViewUtil.showKeyboard(authoEditText!!)
        }
    }
}
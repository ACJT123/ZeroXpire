package my.edu.tarc.zeroxpire.recipe.adapter

import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.recipe.Comment
import my.edu.tarc.zeroxpire.recipe.viewHolder.CommentRecyclerViewHolder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class CommentRecyclerViewAdapter(
    private val commentArrayList: ArrayList<Comment>
    ) : RecyclerView.Adapter<CommentRecyclerViewHolder>() {

    // declaration
    private lateinit var parentContext: Context
    private lateinit var auth: FirebaseAuth

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentRecyclerViewHolder {
        parentContext = parent.context
        auth = FirebaseAuth.getInstance()

        val view = LayoutInflater.from(parentContext).inflate(viewType, parent, false)
        return CommentRecyclerViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.comment_frame
    }

    override fun onBindViewHolder(holder: CommentRecyclerViewHolder, position: Int) {
        // declaration: views
        val currentComment = commentArrayList[position]

        val usernameTextView = holder.getView().findViewById<TextView>(R.id.usernameTextView)
        val dateTextView = holder.getView().findViewById<TextView>(R.id.dateTextView)
//        val likesCountTextView = holder.getView().findViewById<TextView>(R.id.likesCountTextView)
        val displayedCommentEditText = holder.getView().findViewById<EditText>(R.id.displayedCommentEditText)

        usernameTextView.text = currentComment.username

        val sdf = SimpleDateFormat(parentContext.getString(R.string.date_format), Locale.getDefault())
        dateTextView.text = sdf.format(currentComment.dateTime)

        displayedCommentEditText.inputType = InputType.TYPE_NULL
        displayedCommentEditText.setText(currentComment.comment)

//        likesCountTextView.text = currentComment.likesCount.toString()

        //TODO
        // set likeImageView on click
        // set replyImageView
   }


    override fun getItemCount(): Int {
        return commentArrayList.size
    }

}